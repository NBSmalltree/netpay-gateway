package com.atom.netpaygateway.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Socket连接管理 Netty
 *
 * @author Tom
 * @date 10/3/2025
 */
@Slf4j
@Service
public class SocketClientNetty implements ApplicationListener<ContextClosedEvent>, HealthIndicator {

    /**
     * Socket 服务器地址（后续数据库表获取）
     */
    private final String host = "127.0.0.1";

    /**
     * Socket 服务器端口（后续数据库表获取）
     */
    private final int port = 8081;

    /**
     * 处理进程
     */
    private final EventLoopGroup group;

    /**
     * 状态标记位
     */
    private final AtomicBoolean active = new AtomicBoolean(true);

    /**
     * 带参构造函数
     *
     * @param httpSocketGroup 参数说明
     */
    public SocketClientNetty(EventLoopGroup httpSocketGroup) {
        this.group = httpSocketGroup;
    }

    /**
     * Netty 客户端发送 Socket 请求
     *
     * @param message 发送报文体
     * @return 返回值
     */
    public Mono<String> sendMessage(String message) {
        if (!active.get()) {
            return Mono.error(new IllegalStateException("Client is shutting down"));
        }

        return Mono.create(stringMonoSink -> {
            try {
                final AtomicBoolean responseReceived = new AtomicBoolean(false);
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast(
                                        new StringEncoder(CharsetUtil.UTF_8),
                                        new StringDecoder(CharsetUtil.UTF_8),
                                        new SimpleChannelInboundHandler<String>() {
                                            @Override
                                            protected void channelRead0(ChannelHandlerContext ctx, String s) {
                                                log.info("Received Response from Socket: {}", s);
                                                responseReceived.set(true);
                                                try {
                                                    // 正常返回数据
                                                    stringMonoSink.success(s);
                                                } catch (IllegalStateException e) {
                                                    log.warn("Sink already completed");
                                                }
                                                ctx.close();
                                            }

                                            @Override
                                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                                log.error("Exception caught: {}", cause.getMessage());
                                                if (!responseReceived.get()) {
                                                    try {
                                                        // 捕获异常并返回
                                                        stringMonoSink.error(cause.getCause());
                                                    } catch (IllegalStateException e) {
                                                        log.warn("Sink already completed");
                                                    }
                                                }
                                                ctx.close();
                                            }
                                        });
                            }
                        });
                ChannelFuture future = bootstrap.connect(host, port).sync();
                future.channel().writeAndFlush(message);
                future.channel().closeFuture().addListener((ChannelFutureListener) f -> {
                    // 仅在未收到响应时触发错误
                    if (!responseReceived.get()) {
                        try {
                            // 捕获异常并返回
                            stringMonoSink.error(new RuntimeException("Socket connection closed without response"));
                        } catch (IllegalStateException e) {
                            log.warn("Sink already completed");
                        }
                    }
                });

                // 设置超时机制，避免无限等待
                stringMonoSink.onDispose(() -> {
                    if (!future.isDone()) {
                        // 关闭通道
                        future.channel().close();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                try {
                    stringMonoSink.error(e);
                } catch (IllegalStateException ex) {
                    log.warn("Sink already completed");
                }
            }
        }).timeout(Duration.ofSeconds(5)).cast(String.class);
    }

    /**
     * 关闭函数
     */
    @PreDestroy
    public void shutdown() {
        if (active.compareAndSet(true, false)) {
            log.info("Initiating graceful shutdown...");
            // 停止接收新请求：立即停止接收新请求（0秒），允许2秒完成存量请求处理
            group.shutdownGracefully(0, 2, TimeUnit.SECONDS)
                    .addListener(future -> {
                        if (future.isSuccess()) {
                            log.info("Netty Socket Client resource released");
                        } else {
                            log.error("Shutdown error", future.cause());
                        }
                    });
        }
    }

    /**
     * Spring 关闭时优先执行
     *
     * @param event 参数说明
     */
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        shutdown();
    }

    /**
     * Spring 健康检查
     *
     * @return 返回值
     */
    @Override
    public Health health() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("status", active.get() ? "SERVING" : "OUT_OF_SERVICE");

        // 尝试获取实时线程数
        int currentThreads = -1;
        try {
            if (group instanceof MultithreadEventLoopGroup) {
                currentThreads = ((MultithreadEventLoopGroup) group).executorCount();
            }
        } catch (NoSuchMethodError e) {
            // 兼容旧版本
            currentThreads = Runtime.getRuntime().availableProcessors() * 2;
        }

        details.put("threads", currentThreads > 0 ? String.valueOf(currentThreads) : "unavailable");

        return active.get() ? Health.up().withDetails(details).build() :
                Health.down().withDetails(details).build();
    }
}

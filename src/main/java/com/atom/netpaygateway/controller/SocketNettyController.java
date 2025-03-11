package com.atom.netpaygateway.controller;

import com.atom.netpaygateway.codec.XmlMessageDecoder;
import com.atom.netpaygateway.service.SocketToHttpService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Socket Netty 实现
 *
 * @author Tom
 * @date 11/3/2025
 */
@Slf4j
@Service
public class SocketNettyController implements ApplicationListener<ContextRefreshedEvent>, DisposableBean {

    /**
     * 已关闭标记位，防止重复关闭
     */
    private volatile boolean isShutdown = false;

    /**
     * 监听端口
     */
    @Value("${gateway.listen.socket.netty.port}")
    private int port;

    /**
     * Socket To Http 服务
     */
    @Autowired
    private SocketToHttpService service;

    /**
     * 主线程
     */
    private EventLoopGroup bossGroup;

    /**
     * 工作线程
     */
    private EventLoopGroup workerGroup;

    /**
     * 服务端通道
     */
    private Channel serverChannel;

    /**
     * 启动
     * 当Spring容器加载到最后一步，准备启动前发布ContextRefreshedEvent事件
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("准备异步启动Socket -> Http Netty 服务");
        new Thread(this::startServer, "netty-starter").start();
    }

    /**
     * 重启Socket -> Http Netty 服务
     */
    public void restartServer() {
        log.info("准备重启Socket -> Http Netty 服务");
        if (bossGroup != null && !bossGroup.isShuttingDown()) {
            log.warn("Socket -> Http Netty Server is already running, skip restart.");
            return;
        }
        new Thread(this::startServer, "netty-starter").start();
    }

    /**
     * 启动Socket -> Http Netty 服务
     */
    public void startServer() {
        long startTime = System.currentTimeMillis();
        try {
            // 根据CPU核数动态设置
            int threadCount = Runtime.getRuntime().availableProcessors() * 2;
            bossGroup = new NioEventLoopGroup(1,
                    new DefaultThreadFactory("netty-socket-server-boss", true));
            workerGroup = new NioEventLoopGroup(threadCount,
                    new DefaultThreadFactory("netty-socket-server-worker", true));
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new XmlMessageDecoder());
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new SimpleChannelInboundHandler<String>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String s) {
                                    log.info("Socket -> Http 请求信息是：{}", s);
                                    service.sendMessageWithNetty(ctx, s);
                                }
                            });
                        }

                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                            log.error("Socket -> Http Netty 服务异常" + cause.getMessage());
                            ctx.writeAndFlush("Socket -> Http Netty 服务异常" + cause.getMessage() + "\r\n");
                            ctx.close();
                        }
                    })
                    // 性能优化参数
                    // 支持端口快速重用
                    .option(ChannelOption.SO_REUSEADDR, true)
                    // 连接超时配置：3000ms连接超时保护
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    // 禁用Nagle算法，关闭延迟发送，立即发送数据包
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.SO_RCVBUF, 1024 * 1024 * 4)
                    // 水位线控制：32KB低水位线/64KB高水位线，防止OOM
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                            new WriteBufferWaterMark(32 * 1024, 64 * 1024));
            // 异步绑定端口
            ChannelFuture bindFuture = bootstrap.bind(port).sync();
            serverChannel = bindFuture.channel();
            log.info("Socket -> Http Netty 服务:{} 启动成功，耗时：{}ms", port, System.currentTimeMillis() - startTime);

            // 重启Netty时需重制该开关
            isShutdown = false;

            // 注册关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            // 阻塞知道通道关闭（在独立线程中执行）
            serverChannel.closeFuture().syncUninterruptibly();
        } catch (Exception e) {
            log.error("Critical error occurred during Netty Server startup", e);
            shutdown();
        }
    }

    /**
     * 销毁函数
     */
    @Override
    public void destroy() {
        shutdown();
    }

    /**
     * 关闭Netty服务
     */
    public synchronized void shutdown() {
        // 防止重复关闭
        if (isShutdown) {
            return;
        }
        isShutdown = true;

        log.info("Shutting down Netty Server...");
        // 同步关闭通道
        shutdownChannelSync();
        // 启动优雅关闭
        shutdownEventLoopGroups();
        // 等待完全关闭
        waitForNettyShutdown();

        log.info("Netty Server shutdown completed.");
    }

    /**
     * 同步等待通道关闭
     */
    private void shutdownChannelSync() {
        if (serverChannel != null) {
            try {
                serverChannel.close().sync();
                log.info("Netty Server channel closed successfully.");
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for Netty Server channel to close.");
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 关闭EventLoopGroups
     */
    private void shutdownEventLoopGroups() {
        if (!bossGroup.isShutdown()) {
            // 100ms静默期+300ms超时
            bossGroup.shutdownGracefully(100, 300, TimeUnit.MILLISECONDS)
                    .addListener(f -> log.info("Boss group shutdown complete"));
        }
        if (!workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully(100, 300, TimeUnit.MILLISECONDS)
                    .addListener(f -> log.info("Worker group shutdown complete"));
        }
    }

    /**
     * 等待Netty关闭后，才能执行Spring容器的关闭
     */
    private void waitForNettyShutdown() {
        try {
            if (!bossGroup.isShutdown()) {
                bossGroup.terminationFuture().sync();
            }
            if (!workerGroup.isShutdown()) {
                workerGroup.terminationFuture().sync();
            }
            log.info("Netty Resources fully released");
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for Netty resources to release");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Netty 健康状态检查
     *
     * @return 返回值
     */
    public boolean isHealthy() {
        return !bossGroup.isShutdown() && !workerGroup.isShutdown();
    }
}

package com.atom.netpaygateway.config;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Socket连接池/Netty配置
 *
 * @author Tom
 * @date 7/3/2025
 */
@Configuration
public class SocketConfig {

    /**
     * netty Socket 客户端注入
     *
     * @return 返回值
     */
    @Bean(destroyMethod = "shutdownGracefully")
    public EventLoopGroup httpSocketGroup() {
        // 根据CPU核数动态设置
        int threadCount = Runtime.getRuntime().availableProcessors() * 2;
        return new NioEventLoopGroup(threadCount,
                new DefaultThreadFactory("netty-socket-client-worker", true) {
                    private final AtomicInteger id = new AtomicInteger(0);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = super.newThread(r);
                        thread.setName("netty-socket-client-" + id.getAndIncrement());
                        return thread;
                    }
                });
    }
}

package com.atom.netpaygateway.client;

import com.atom.netpaygateway.config.PoolConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 连接池实现
 *
 * @author Tom
 * @date 7/3/2025
 */
@Slf4j
@EnableAsync
@Configuration
public class ConnectionPool {

    /**
     * 来账线程池
     *
     * @return 来账线程池
     *
     * @author Tom
     * @date 10/3/2025
     */
    @Bean("httpThreadPoolExecutor")
    public ThreadPoolTaskExecutor httpThreadPoolExecutor(PoolConfig config) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getHttpPoolCoreSize());
        executor.setMaxPoolSize(config.getHttpPoolMaxSize());
        executor.setKeepAliveSeconds(config.getHttpPoolKeepAliveTime());
        executor.setQueueCapacity(config.getHttpPoolQueneSize());
        executor.setThreadNamePrefix(config.getHttpPoolPreFixName());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(config.getHttpPoolAwaitTerminationSeconds());
        log.info("初始化来账线程池：核心线程：{}，最大线程：{}，空闲进程销毁时间：{}，队列深度：{}，进程名：{}，优雅关闭最长等待时间：{}",
                config.getHttpPoolCoreSize(), config.getHttpPoolMaxSize(), config.getHttpPoolKeepAliveTime(),
                config.getHttpPoolQueneSize(), config.getHttpPoolPreFixName(),
                config.getHttpPoolAwaitTerminationSeconds());
        return executor;
    }

}

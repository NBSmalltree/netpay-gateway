package com.atom.netpaygateway.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 连接池Config
 *
 * @author Tom
 * @date 10/3/2025
 */
@Data
@Configuration
public class PoolConfig {

    /**
     * 核心线程数-Http
     */
    @Value("${threadPool.http.coreSize:10}")
    private Integer httpPoolCoreSize;

    /**
     * 最大线程数-Http
     */
    @Value("${threadPool.http.maxSize:100}")
    private Integer httpPoolMaxSize;

    /**
     * 线程池队列深度-Http
     */
    @Value("${threadPool.http.QueneSize:10}")
    private Integer httpPoolQueneSize;

    /**
     * 空闲进程销毁时间-Http
     */
    @Value("${threadPool.http.keepAliveTime:180}")
    private Integer httpPoolKeepAliveTime;

    /**
     * 进程名前缀-Http
     */
    @Value("${threadPool.http.preFixName:NETPAY-http-}")
    private String httpPoolPreFixName;

    /**
     * 优雅关闭最长等待时间-Http
     */
    @Value("${threadPool.http.AwaitTerminationSeconds:30}")
    private Integer httpPoolAwaitTerminationSeconds;
}

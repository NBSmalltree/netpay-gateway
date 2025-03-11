package com.atom.netpaygateway.config;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Http 异步客户端配置
 *
 * @author Tom
 * @date 11/3/2025
 */
@Configuration
public class AsyncHttpClientConfig {

    /**
     * 连接超时时间 秒
     */
    @Value("${gateway.forward.http.connectTimeout}")
    private Integer connectTimeout;

    /**
     * 读取超时时间 秒
     */
    @Value("${gateway.forward.http.readTimeout}")
    private Integer readTimeout;

    /**
     * 最大连接数
     */
    @Value("${gateway.forward.http.maxConnections}")
    private Integer maxConnections;

    /**
     * 最大连接数_单节点
     */
    @Value("${gateway.forward.http.maxConnectionsPerHost}")
    private Integer maxConnectionsPerHost;

    /**
     * 空闲进程销毁时间
     */
    @Value("${gateway.forward.http.keepAliveTime}")
    private Integer keepAliveTime;

    /**
     * 创建异步客户端
     */
    @Bean(destroyMethod = "close")
    public AsyncHttpClient asyncHttpClient() {
        return Dsl.asyncHttpClient(Dsl.config()
                .setConnectTimeout(connectTimeout * 1000)
                .setRequestTimeout(readTimeout * 1000)
                .setMaxConnections(maxConnections)
                .setMaxConnectionsPerHost(maxConnectionsPerHost)
                .setPooledConnectionIdleTimeout(keepAliveTime * 1000));
    }
}

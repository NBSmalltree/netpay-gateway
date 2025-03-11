package com.atom.netpaygateway.client;

import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Http 连接管理-Netty
 *
 * @author Tom
 * @date 11/3/2025
 */
@Slf4j
@Service
public class HttpClientNetty {

    /**
     * URL
     */
    @Value("${gateway.forward.http.url}")
    private String url;

    /**
     * 连接超时时间 秒
     */
    @Value("${gateway.forward.http.connectTimeout}")
    private Integer connectTimeout;

    /**
     * 异步Http客户端
     */
    private final AsyncHttpClient httpClient;

    /**
     * 带参构造函数
     *
     * @param httpClient 参数说明
     */
    public HttpClientNetty(AsyncHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * 实际发送Http
     *
     * @param message 报文体
     * @return 返回值
     */
    public CompletableFuture<String> sendMessage(String message) {
        Request request = new RequestBuilder()
                .setMethod("POST")
                .setUrl(url)
                .setRequestTimeout(connectTimeout * 1000)
                .setHeader("Content-Type", "application/xml")
                .setBody(message)
                .build();

        return httpClient.executeRequest(request)
                .toCompletableFuture()
                .thenApply(this::handleResponse);
    }

    /**
     * 应答处理
     *
     * @param response 响应报文
     * @return 返回值
     */
    private String handleResponse(Response response) {
        if (response.getStatusCode() >= 400) {
            throw new RuntimeException("HTTP请求失败，状态码：" + response.getStatusCode());
        }
        return response.getResponseBody();
    }
}

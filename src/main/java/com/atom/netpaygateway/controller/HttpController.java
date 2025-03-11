package com.atom.netpaygateway.controller;

import com.atom.netpaygateway.controller.response.HelloResponse;
import com.atom.netpaygateway.service.HttpToSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

/**
 * HTTP入口处理
 *
 * @author Tom
 * @date 7/3/2025
 */
@Slf4j
@RestController
public class HttpController {

    /**
     * Http 线程池
     */
    @Autowired
    @Qualifier("httpThreadPoolExecutor")
    private ThreadPoolTaskExecutor httpPoolExecutor;

    /**
     * Http 处理服务
     */
    @Autowired
    private HttpToSocketService service;

    /**
     * 测试接口HELLO
     *
     * @param name 名字
     * @return 返回值
     */
    @PostMapping(value = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    public HelloResponse helloWorld(@RequestBody @Nullable String name) {
        String message = "Hello " + (name != null && !name.isEmpty() ? name : "World");
        return new HelloResponse(message);
    }

    /**
     * Http 线程池方案来账接收
     *
     * @param requestMessage 请求的Body
     * @return 同步响应
     */
    @PostMapping(value = "${gateway.feListen.url:/netpay-gateway/recv-from-fe}", produces = "text/plain")
    public DeferredResult<String> recvMsg(@RequestBody String requestMessage) {
        DeferredResult<String> deferredResult = new DeferredResult<>(5000L);

        httpPoolExecutor.execute(() -> {
            log.info("HTTP 请求信息是：{}", requestMessage);
            try {
                String response = service.sendMessageWithPool(requestMessage);
                log.info("Socket 响应信息是：{}", response);
                deferredResult.setResult(response);
            } catch (Exception e) {
                deferredResult.setErrorResult("Error:" + e.getMessage());
            }
        });

        return deferredResult;
    }

    /**
     * Http Netty方案来账接收
     *
     * @param requestMessage 请求的Body
     * @return 返回值
     */
    @PostMapping(value = "${gateway.feListen.url1:/netpay-gateway/recv-from-fe1}", produces = "text/plain")
    public Mono<String> recvMsg1(@RequestBody String requestMessage) {
        log.info("Http 请求信息是：{}", requestMessage);
        return service.sendMessageWithNetty(requestMessage)
                .doOnNext(response -> log.info("Http 响应信息是：{}", response))
                .switchIfEmpty(Mono.just("No response from socket")); // 防止空响应
    }

}

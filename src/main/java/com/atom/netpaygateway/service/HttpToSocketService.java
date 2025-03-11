package com.atom.netpaygateway.service;

import com.atom.netpaygateway.client.SocketClientNetty;
import com.atom.netpaygateway.client.SocketClientSingle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Http -> Socket 转换逻辑
 *
 * @author Tom
 * @date 7/3/2025
 */
@Service
public class HttpToSocketService {

    /**
     * Socket 客户端-单笔发送
     */
    @Autowired
    private SocketClientSingle socketClientSingle;

    /**
     * Socket 客户端-Netty
     */
    @Autowired
    private SocketClientNetty socketClientNetty;

    /**
     * Http 转换到 Socket 请求实现-线程池方案
     *
     * @param request 参数说明
     * @return 返回值
     */
    public String sendMessageWithPool(String request) {
        return socketClientSingle.sendMessage(request);
    }

    /**
     * Http 转换到 Socket 请求实现-Netty方案
     *
     * @param request 参数说明
     * @return 返回值
     */
    public Mono<String> sendMessageWithNetty(String request) {
        return socketClientNetty.sendMessage(request);
    }
}

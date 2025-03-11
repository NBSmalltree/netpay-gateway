package com.atom.netpaygateway.service;

import com.atom.netpaygateway.client.HttpClientNetty;
import com.atom.netpaygateway.client.HttpClientSingle;
import com.atom.netpaygateway.enums.EnumRespCode;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Socket -> Http 转换逻辑
 *
 * @author Tom
 * @date 11/3/2025
 */
@Slf4j
@Service
public class SocketToHttpService {

    /**
     * Http 请求实现-单笔
     */
    @Autowired
    private HttpClientSingle httpClientSingle;

    /**
     * Http 请求实现-Netty
     */
    @Autowired
    private HttpClientNetty httpClientNetty;

    /**
     * Socket 转换到 Http 请求实现-线程池方案
     *
     * @param request 参数说明
     * @return 返回值
     */
    public String sendMessageWithPool(String request) {
        String response = httpClientSingle.sendMessage(request);
        if (request == null) {
            response = EnumRespCode.FAIL.getCode();
        }
        return response;
    }

    /**
     * Socket 转换到 Http 请求实现-Netty方案
     *
     * @param ctx 参数说明
     * @param s 参数说明
     */
    public void sendMessageWithNetty(ChannelHandlerContext ctx, String s) {
        httpClientNetty.sendMessage(s).whenComplete((response, ex) -> {
            if (ex == null) {
                log.info("Socket -> Http 响应信息是:{}", response);
                ctx.writeAndFlush(response + "\r\n");
            } else {
                log.warn("Socket -> Http 响应信息是:{}", ex.getMessage());
                ctx.writeAndFlush("Error:" + ex.getMessage() + "\r\n");
            }
        });
    }
}

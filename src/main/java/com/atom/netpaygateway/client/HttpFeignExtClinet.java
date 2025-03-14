package com.atom.netpaygateway.client;

import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * Http发送Feign
 *
 * @author Atom
 * @date 2020/3/16
 */
public interface HttpFeignExtClinet {

    /**
     * 发送消息
     *
     * @param message 消息
     * @return String
     */
    @Headers({"Content-Type: application/xml;charset=UTF-8"})
    @RequestLine("POST ")
    @Body("{body}")
    String sendMessage(@Param("body") String message);
}

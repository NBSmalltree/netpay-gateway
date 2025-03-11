package com.atom.netpaygateway.service;

import com.atom.netpaygateway.client.HttpClientSingle;
import com.atom.netpaygateway.enums.EnumRespCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Socket -> Http 转换逻辑
 *
 * @author Tom
 * @date 11/3/2025
 */
@Service
public class SocketToHttpService {

    /**
     * Http 请求实现
     */
    @Autowired
    private HttpClientSingle httpClientSingle;

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
}

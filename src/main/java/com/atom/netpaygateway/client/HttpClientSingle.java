package com.atom.netpaygateway.client;

import com.atom.netpaygateway.enums.EnumRespCode;
import feign.Feign;
import feign.Request;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Http 连接管理-单笔请求
 *
 * @author Tom
 * @date 11/3/2025
 */
@Slf4j
@Service
public class HttpClientSingle {

    /**
     * URL
     */
    @Value("${client.http.url}")
    private String url;

    /**
     * 连接超时时间 秒
     */
    @Value("${client.http.connectTimeout}")
    private Integer connectTimeout;

    /**
     * 读取超时时间 秒
     */
    @Value("${client.http.readTimeout}")
    private Integer readTimeout;

    /**
     * 是否需重发，默认 否
     */
    @Value("${client.http.canResend}")
    private boolean canResend;

    /**
     * 单笔发送 Http 请求
     *
     * @param message 发送报文体
     * @return 返回值
     */
    public String sendMessage(String message) {
        log.info("发送Http：{}，连接超时：{}秒，接收超时：{}秒", url, connectTimeout, readTimeout);
        HttpFeignExtClinet feignExtClinet = Feign.builder()
                .encoder(new Encoder.Default())
                .decoder(new Decoder.Default())
                .options(new Request.Options(connectTimeout, TimeUnit.SECONDS, readTimeout, TimeUnit.SECONDS, true))
                .retryer(canResend ? new Retryer.Default() : Retryer.NEVER_RETRY)
                .target(HttpFeignExtClinet.class, url);

        try {
            return feignExtClinet.sendMessage(message);
        } catch (Exception e) {
            log.warn("Http 调用异常", e);
            return EnumRespCode.FAIL.getCode();
        }
    }
}

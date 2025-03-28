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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    @Value("${gateway.forward.http.url}")
    private String url;

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
     * 是否需重发，默认 否
     */
    @Value("${gateway.forward.http.canResend}")
    private boolean canResend;

    /**
     * 单笔发送 Http 请求-使用原始Http
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

    /**
     * 单笔发送 Http 请求-使用原始Http
     *
     * @param message 发送报文体
     * @return 返回值
     */
    public String sendMessageOri(String message) {
        log.info("发送Http：{}，连接超时：{}秒，接收超时：{}秒", url, connectTimeout, readTimeout);

        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/xml;charset=UTF-8");
            con.setDoOutput(true);
            con.setConnectTimeout(connectTimeout * 1000);
            con.setReadTimeout(readTimeout * 1000);

            try (OutputStream os = con.getOutputStream()) {
                byte[] input = message.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = con.getResponseCode();
            log.info("Http 响应码 : {}", responseCode);

            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine).append("\n");
                }
                return response.toString();
            }
        } catch (Exception e) {
            log.warn("Http 调用异常", e);
            if (canResend) {
                log.info("尝试重新发送请求");
                return sendMessage(message); // 递归重试
            }
            return EnumRespCode.FAIL.getCode();
        }
    }
}


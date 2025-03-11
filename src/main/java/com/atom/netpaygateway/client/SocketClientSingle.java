package com.atom.netpaygateway.client;

import com.atom.netpaygateway.enums.EnumRespCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Socket连接管理（BIO/NIO/Netty）
 *
 * @author Tom
 * @date 7/3/2025
 */
@Slf4j
@Service
public class SocketClientSingle {

    /**
     * 单笔发送 Socket 请求
     *
     * @param message 发送报文体
     * @return 响应信息
     *
     * @author Tom
     * @date 10/3/2025
     */
    public String sendMessage(String message) {
        try (Socket socket = new Socket("127.0.0.1", 12345);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            // 发送消息到 Socket 服务
            out.println(message);
            // 接收 Soocket 应答
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString().trim();
        } catch (IOException ex) {
            log.warn("Socket 调用异常", ex);
            return EnumRespCode.FAIL.getCode();
        }
    }
}

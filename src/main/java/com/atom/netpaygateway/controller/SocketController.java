package com.atom.netpaygateway.controller;

import com.atom.netpaygateway.constants.Constants;
import com.atom.netpaygateway.service.SocketToHttpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Socket 入口处理
 *
 * @author Tom
 * @date 11/3/2025
 */
@Slf4j
@Service
public class SocketController {

    /**
     * socket 监听端口
     */
    @Value("${gateway.listen.socket.port:11111}")
    private int port;

    /**
     * socket 处理
     */
    @Autowired
    private SocketToHttpService socketToHttpService;

    /**
     * socket 处理线程池
     */
    @Autowired
    @Qualifier("socketThreadPoolExecutor")
    private ThreadPoolTaskExecutor executor;

    /**
     * Socket 线程池方案来账接收
     */
    @PostConstruct
    public void start() {
        // 内部的 while(true) 会占用主进程，所以提交一个新线程去处理
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                log.info("Socket 监听已启动，端口：{}", port);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    executor.execute(() -> {
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                             OutputStream out = clientSocket.getOutputStream()) {
                            StringBuilder request = new StringBuilder();
                            StringBuilder endMarker = new StringBuilder();
                            int ch;
                            while ((ch = in.read()) != -1) {
                                request.append((char) ch);
                                endMarker.append((char) ch);

                                if (endMarker.length() >= Constants.END_MARK.length()) {
                                    String lastChars = endMarker.substring(endMarker.length() - Constants.END_MARK.length());
                                    if (lastChars.equals(Constants.END_MARK)) {
                                        break;
                                    }
                                }
                            }
                            log.info("Socket -> Http 请求信息是：{}", request);

                            String response = socketToHttpService.sendMessageWithPool(request.toString());
                            log.info("Socket -> Http 响应信息是：{}", response);
                            out.write(response.getBytes());
                            out.flush();
                        } catch (Exception e) {
                            log.error("Socket 处理失败：{}", e.getMessage());
                        } finally {
                            try {
                                clientSocket.close();
                            } catch (Exception e) {
                                log.error("Socket 关闭失败：{}", e.getMessage());
                            }
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Socket 线程池执行失败：{}", e.getMessage());
            }
        }).start();
    }
}

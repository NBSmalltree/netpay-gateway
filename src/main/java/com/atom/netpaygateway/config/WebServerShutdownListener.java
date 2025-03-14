package com.atom.netpaygateway.config;

import com.atom.netpaygateway.controller.SocketNettyController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * 中间件关闭时的顺序控制
 *
 * @author Tom
 * @date 14/3/2025
 */
@Component
public class WebServerShutdownListener implements ApplicationListener<ContextClosedEvent> {

    /**
     * NettyServer
     */
    @Autowired
    private SocketNettyController socketNettyController;

    /**
     * Spring关闭时，先关闭NettyServer
     *
     * @param event 事件
     */
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        socketNettyController.shutdown();
    }
}

# 网关工程说明

本工程预期实现Http/TCP(Socket)之间的相互转换，分别提供了线程池方案和netty方案。

## 1 Http -> Socket

### 1.1 线程池方案

**总体流程**

Spring主线程池接收到Http请求，提交到Http线程池，转发到Socket的单笔请求，同步接收响应信息。

**配置如下**

线程池配置：
 - 核心线程：10
 - 最大线程：100
 - 队列深度：10

监听url：/netpay-gateway/recv-from-fe

转发的socket：127.0.0.1:12345

### 1.2 netty方案

### 1.3 测试方案

 1. 通过vscode启动socket监听
 2. foxapi发送"http线程池测试"
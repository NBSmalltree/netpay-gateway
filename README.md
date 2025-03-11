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

**总体流程**

Spring主线程池接收到Http请求，提交到Netty客户端，转发后端Socket服务，同步接收响应信息。

**配置如下**

Netty客户端：
 - 工作进程：CPU核数 * 2

监听url：/netpay-gateway/recv-from-fe1

转发的socket：127.0.0.1:8081

### 1.3 测试方案

 1. 通过vscode启动socket监听
 2. foxapi发送"http线程池测试"
 
## 2 Socket -> Http

### 2.1 线程池方案

**总体流程**

Spring启动时用一个新线程一直监听对应Socket端口的的请求，提交到Http线程池，转发到Http单笔请求，同步接收响应信息。

**配置如下**

线程池配置：
 - 核心线程：10
 - 最大线程：100
 - 队列深度：10

监听端口：11111

转发的url：http://localhost:9001/mock/singleResponse

### 2.2 netty方案

### 2.3 测试方案

1. 启动http监听，通过终端启动http监听
2. vscode发送"socketSend"
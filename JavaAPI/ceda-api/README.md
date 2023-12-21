# CEDA Java API

基本概念

CEDA Java API采用类似socket的C/S架构, 包含了客户端API, 服务端API, 
及消息API, 同时为集群功能提供了集群API。 

在概念上, CEDA的客户端并不是应用系统语言中的客户端, 而是CEDA服务的
使用端或消费者, 一个CEDA应用可以既包含服务端也包含客户端，如下图:

``` 
Java API 实践简化示意图

            CEDA APP 1                              CEDA APP 2
           ________________                       ________________
          |CEDA Server API|<------|       |--->  |CEDA Server API|
      |---|CEDA Client API|       |       |      |---------------|
      |   |---------------|       |       |             ^       
      |                           |       |             |
      |                           |       |             |         
      |     CEDA APP 3            |       |             |  
      |     _________________     |       |             |  
      |--->| CEDA Server API|     |       |          CEDA APP 4
           | CEDA Client API|-----|       |       ________________
           |----------------|-------------|      |CEDA Client API|
                                                 |---------------|

```     
服务端API: 

com.adaptiveMQ.server是服务端API, 工作模型如下:

1. 启动阶段

   1.1 向集群注册自己, 包括地址和名称(通过集群API, 可选)
   
   1.2 通过构造MQServer创建端口监听, 传入IServerConnectionListener用于接收客户端连接状态变更, 
   传入IClusterClient代表可集群客户端, 传入IServerLoginHandler用于接收客户端登录请求 
   
2. 会话建立阶段
    
    2.1 接受用户连接
        
    2.2 接受客户端登录并进行校验
    
    校验动作由业务层构造的IServerLoginHandler实例完成, 
    通过onUserLogin返回值来通知平台校验是否通过。
    IServerConnection代表客户端连接, 由框架从内部实例化,
    业务端可以对其设置回调函数用于接收消息setMessageHandler
    以及订阅请求setSubscriptionHandler
    
    2.3 通过保活心跳等方式维持会话

3. 会话-响应请求

    3.1 接受客户端发起的消息主题订阅
    
    3.2 接受用户RPC请求并回应
    
4. 会话-消息发布

    4.1 填写消息主题并发布(框架自动根据订阅关系匹配推送)
    
    4.2 向制定客户端发送消息

5. 会话终结

    5.1 接收客户端的登出消息或长连接断开
    
    5.2 清理客户端相关数据 

客户端API

com.adaptiveMQ.client是客户端API, 工作模型如下:

1. 启动阶段

   1.1 向集群注册器查找服务端真实地址等注册信息 (通过集群API, 可选)
   
2. 会话建立阶段
   
   2.1 向服务端创建连接
   
   2.2 登录服务端
   
   2.2 注册入消息回调函数

3. 会话阶段
   
   3.1 向服务端发起RPC请求
   
   3.2 向服务端发起广播消息订阅请求

4. 会话终结

    5.1 主动断开会话或者被动关闭
    

集群API

com.adaptiveMQ.cluster是集群API, 
集群API由注册中心连接器RegisterHandler和集群客户端ClusterClient组成, 
前者实现注册中心的底层驱动, 后者实现上层的服务注册发现功能, 

服务端通过：

1. 创建RegisterHandler::connectRegister来连接到注册中心

2. 传入RegisterHandler作为底层驱动构造ClusterClient

3. 设置ServiceInfo描述自身的服务类型

4. 通过ClusterClient::registService向注册中心注册ServiceInfo

5. 通过ClusterClient::setServiceInfor来向注册中心更新ServiceInfo

    例如自身服务客户端数发生变化, 那么集群客户端所观测到的集群状态也应当发生变化

6. 通过 ClusterClient::addListeneraddListener(IClusterListener)注册回调接收注册中心变更通知

7. 通过ClusterClient::getWillWorkService() 在注册中心状态
   变更时查询自身是否被集群算法指定为下一个工作节点。
   
客户端通过：

1. 创建RegisterHandler::connectRegister来连接到注册中心

2. RegisterHandler::getClusterClient(String serviceName)获得对应服务的集群客户端

3. ClusterClient::addListeneraddListener(IClusterListener)注册回调接收注册中心变更通知

4. 通过 ClusterClient::getWorkService() 用于查询当前可用的服务列表

CEDA集群有三种类型, "STANDBY", "BALANCE", "ROUNDROBIN", "ALL"
由服务端通过ServiceType向注册中心声明, 
其中

STANDBY是一主多备模式, 集群中只有一个主工作节点, 其余节点处于待机STANDBY状态 

BALANCE是根据客户端连接数决定权重的负载均衡多活模式, 连接数由服务端通过setServiceInfor实时更新至注册中心

ROUNDROBIN在BALANCE基础上加入了RR算法

ALL表示完全多活模式

com.adaptiveMQ.message是消息API  


CEDA Java API使用IMessage/Message/ControlMessage的体系来包装所有的数据, 
Message表示业务消息由业务层读写, ControlMessage表示控制消息由框架读写
Message包含四个重要字段分别是:

SvrID

表示服务在集群中的唯一ID, 当CEDA服务端向注册中心注册时会填写Name, 即SvrID, 
当两个节点间使用点对点直连模式时, 该字段没有实际作用, 但当使用消息代理连接时, 该字段起到路由的作用。
即当客户端向服务端发起RPC请求或者订阅时, 如果经过消息代理, 那么代理将根据该
字段来找到对应的服务端。

Destination

该字表示消息的主题, 在REQ/REP的情况下, 该消息由客户端填写, 
服务端根据Destination将消息交给对应的处理函数, 
在PUB/SUB的情况, 该字段由服务端客户端双方填写, 即客户端通过
IClientSession::subscribe(List<BaseDestination> destinationList, IMessageListener listener); 
来订阅感兴趣的消息, 服务端通过Message的Destination来完成消息的匹配投递。

ReplyTo和ConnectionID


该两个字段不需要业务层关注, 这里简述其工作原理, ReplyTo和ConnectionID一起
实现分布式REQ/REP的闭包, ReplyTo由客户端自动填写的随机回信地址, 并将其绑定
REP处理逻辑, 服务端生成REP时将ReplyTo回填作为主题, 用于客户端完成最后处理。
ConnectionID仅在REQ经过消息代理时有意义, 该字段是由服务端分配给客户端的唯一ID,
当客户端发起REQ时, ConnectionID会被自动填入消息并被服务端回填REP, 用于消息代理
在收到REP时候查找对应的客户端连接。


消息匹配逻辑:

CEDA中的消息主题和RabbitMQ类似, 由"."分段表示, 
当分段值为"\*"时, 表示该段会被理解为通配符号, 匹配有限可变长度的任意字符(除了"."本身), 
当分段值为"\*\*"时, 表示该段后续所有匹配均为成功. 具体匹配逻辑参照 
com\adaptiveMQ\utils\Utils.java

传输层

CEDA Java API可以支持多种不同的传输层
client\ProtocolType.java中定义了Java API支持的transport类型, 

服务端通过ServiceInfo的setProtocolType来选择传输层实现。
客户端通过ClientInfo的setProtocol来选择传输层实现。
不同传输层的地址参数不同。

CEDA的传输层被设计为可扩展结构, 

netty transport基于Apache Netty, 实现了基于TCP的传输层, 实现包括:

com.adaptiveMQ.client.transport.tcp
com.adaptiveMQ.server.transport.netty


aeron transport基于Aeron Messaging, 可通过配置使用可靠UDP和共享内存通讯, 实现包括:

com.adaptiveMQ.client.transport.aeron
com.adaptiveMQ.server.transport.aeron


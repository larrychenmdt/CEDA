# CEDA 

CEDA代表Communication Engine for Distributed data Architecture, 
是一款高性能跨语言跨平台的基础通讯引擎，区别于基于消息代理的RabbitMQ或Apache Kafka, CEDA采用弹性的去中心化总线结构, 可对消息代理做透明插拔, 消除了对消息代理的强依赖, 在延迟等关键性能指标上具有传统技术不可比的优势, 功能上, CEDA整合了面向服务架构和消息总线的功能特性, 
对分布式数据架构的所需的基础能力提供了完整抽象, 作为实时交易系统的基础中间件CEDA已经在国内外多家金融机构的生产环境中稳定运转多年。 

CEDA的功能优势:
1. 支持服务注册发现
2. 支持集群高可用和负载均衡
3. 面向服务消息请求应答(REQ/REP)
4. 面向服务主题的发布订阅(PUB/SUB), 支持主题模式匹配, 支持快照和增量传输
5. 支持大消息的自动拆分合并
6. 支持用户认证鉴权
7. 支持消息代理, 包括API(Message) gateway和broker
8. 支持多种传输机制: TCP(S), HTTP(S), WEBSOCKET(S), UDP

CEDA的非功能优势:
1. 高性能(低延迟, 高吞吐), 远超过RabbitMQ/Kafka
2. 灵活的系统架构(支持消息代理和网关等设备的透明插拔, 认证系统的开发集成)
3. 开发简单(通过数个易用API即可进行成快速原型开发或者系统集成)
4. 多语言支持(Java, c++, c#, python, js)
5. 跨平台(Linux, Windows, 国产化操作系统)
6. 依赖少, 编译部署简单
7. 易维护和二次开发(代码规模小, 代码可读性好, 构架简洁)
8. 支持微秒级跨主机通讯(底层可切换Aeron Messaging, 在开发阶段)


``` 
CEDA系统结构

              CEDA Client
            /       |     \   
           /        |      \     
    CEDA Gateway CEDA MQ   CEDA Server(1) ... CEDA Server(N)            
         |          |         |                | 
         |          |         |                | 
     --------------------------------------------
       CEDA API（Java, C++, C#, JS）
     --------------------------------------------
       CEDA protocol
     --------------------------------------------
       Extendable Transport （Netty, ACE, Aeron(exper))

```

CEDA Java API: [开发文档](./JavaAPI/ceda-api/README.md)

CEDA和其他消息中间件的区别

与MQ产品的区别(RABBITMQ/KAFKA)

1. RABBITMQ/KAFKA是对消息代理有强依赖的中心化架构, CEDA是弹性的去中心化架构, 
可在直连模式和代理模式之间透明切换而不影响上层业务
2. RABBITMQ/KAFKA的中心化设计使系统易于管理和维护, 同时也带来了通讯延迟上的问题, 消息代理作为一个物理节点, 
不可避免的存在TCP栈和线程调度等种种开销, CEDA可选择在需要低延迟的路径上直连, 在其他路径上加入消息代理。
3. 通过MQ实现的RPC不仅性能不好且因为缺少连接会话变得较难开发和排错。 

与RPC产品的区别(thrift/grpc)

1. RPC不是完备的消息中间件, 没有对PUB/SUB的支持, 服务注册发现和高可用负载均衡
都需要通过其他的技术集成。
2. CEDA包括了RPC产品的消息序列化, 及同异步REQ/REP功能, 也支持多语言和跨平台。
之外CEDA天然支持PUB/SUB, 高可用集群, 认证鉴权等的消息总线的能力。

与ZMQ的区别

1. ZMQ的设计目的是通用的高性能基础通讯库, 不是功能完备的消息中间件, 
ZMQ只是提供了详细的开发手册来指导开发业务系统所需要的大量功能和非功能特性。
2. CEDA是完备的消息中间件, 可通过灵活配置和API扩展来适应不同的业务需求。CEDA
的传输层被设计为可扩展, ZMQ可作为一种可选的传输层实现加入CEDA。

与Spring Cloud的区别

1. CEDA支持多语言(C++, Java和C#, Js), 不同语言间通过CEDA协议无缝互操作。
2. CEDA支持直连模式的低延迟PUB/SUB。
3. CEDA在处理性能上远超spring cloud。 

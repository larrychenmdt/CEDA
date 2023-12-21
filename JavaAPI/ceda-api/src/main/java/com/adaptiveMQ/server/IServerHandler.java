/*
 *
 *  * Copyright 2003-2022 Beijing XinRong Meridian Limited.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.adaptiveMQ.server;
import com.adaptiveMQ.cluster.IClusterClient;
import com.adaptiveMQ.cluster.ServiceInfo;
import com.adaptiveMQ.message.BaseDestination;
import com.adaptiveMQ.message.Message;

import java.io.Closeable;
import java.util.List;
import java.util.Properties;

public interface IServerHandler extends Closeable
{
    /**
     * 发送消息，对多个连接广播消息
     *
     * @param IMessage msg，发送的消息
     * @return void
     * @throws
     */
    void bind(ServiceInfo msg);

    /**
     * 设置服务端事件回调接口
     *
     * @param IServerConnectionEventListener eventListener, 回调接口
     * @return void
     * @throws
     */
    void setServerConnectionEventListener(IServerConnectionListener eventListener);

    /**
     * 设置服务注册与发现接口
     *
     * @param IClusterClient clusterClient, 回调接口
     * @return void
     * @throws
     */
    void setClusterClient(IClusterClient clusterClient);

    /**
     * 发送消息，对多个连接广播消息
     *
     * @param IMessage msg，发送的消息
     * @return void
     * @throws
     */
    void publishMessage(Message msg);

    IPublisher createPublisher();

    IPublisher createCertifiedMessagePublisher(Properties props);

    /**
     * 加入新的订阅关系
     *
     * @param List<BaseDestination> topicList，主题列表
     * @param IServerConnection connHandler，订阅客户端连接
     * @return void
     * @throws
     */
    void addSubscription(List<BaseDestination> topicList, IServerConnection connHandler);

    /**
     * 删除订阅关系
     *
     * @param List<BaseDestination> topicList，主题列表
     * @param IServerConnection connHandler，订阅客户端连接
     * @return void
     * @throws
     */
    void removeSubscription(List<BaseDestination> topicList, IServerConnection connHandler);

    /**
     * 获取客户端连接
     *
     * @param int connectionID：客户端连接ID号；
     * @return IServerConnection：对应客户端的连接
     * @throws
     */
    IServerConnection getConnection(int connectionID);

    /**
     * 获取所有的客户端
     *
     * @param void；
     * @return List<IServerConnection>，客户端连接列表
     * @throws
     */
    List<IServerConnection> getClients();

    /**
     * 断开所有的客户端
     *
     * @param void;
     * @return void;
     * @throws
     */
    void disConnectClients();

    /**
     * 停止服务，释放资源
     *
     * @param null
     * @return void
     * @throws
     */
    void shutdown();
}

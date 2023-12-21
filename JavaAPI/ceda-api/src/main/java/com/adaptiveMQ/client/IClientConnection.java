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

package com.adaptiveMQ.client;

import java.io.IOException;

public interface IClientConnection
{
    /**
     * 启动连接
     *
     * @param null
     * @return void
     * @throws IOException,ConnectionException
     */
    void start() throws IOException, ConnectionException;

    /**
     * 停止连接
     *
     * @param null
     * @return void
     * @throws
     */
    void stop();

    /**
     * 创建连接Session
     *
     * @param null
     * @return IClientSession：连接Session
     * @throws
     */
    IClientSession createSession();

    /**
     * 设置事件监听
     *
     * @param IEventListener listener：事件监听者
     * @return void
     * @throws
     */
    void addEventListener(IEventListener listener);

    /**
     * 移除事件监听
     *
     * @param IEventListener listener：事件监听者
     * @return void
     * @throws
     */
    void removeEventListener(IEventListener listener);

    /**
     * 获取连接ID
     *
     * @param null
     * @return int：连接Session
     * @throws
     */
    int getConnectionID();

    /**
     * 设置连接ID
     *
     * @param int nConnectionID：连接ID
     * @return void
     * @throws
     */
    void setConnectionID(int nConnectionID);

    void setLoginListener(IMessageListener msgListener);

    /**
     * 设置ping服务间隔，单位秒
     *
     * @param int interval：ping服务间隔
     * @return void
     * @throws
     */
    void setPingInterval(int interval);

    /**
     * 获取客户端ID
     *
     * @param null
     * @return String ：客户端ID
     * @throws
     */
    String getClientID();

    /**
     * 设置客户端ID，用于唯一登录
     *
     * @param String clientID：客户端ID
     * @return void
     * @throws
     */
    void setClientID(String clientID);

    /**
     * 关闭连接
     *
     * @param null
     * @return void
     * @throws
     */
    void close();

    /**
     * 设置重连参数
     *
     * @param int retryTimes：重连次数,long waitTime：重连等待时间
     * @return void
     * @throws
     */
    void setRetryConnect(int retryTimes, long waitTime);

    /**
     * 设置事件，内部使用
     *
     * @param int nCode： 事件号
     * @return void
     * @throws
     */
    void sendEvent(int nCode);

    /**
     * 是否连接成
     *
     * @param null
     * @return boolean： 成功为true，否则false
     * @throws
     */
    boolean isConnected();

    /**
     * 连接后，获取本地地址
     *
     * @param null
     * @return String： 本地地址，或者null
     * @throws
     */
    String getSocketAddress();
}

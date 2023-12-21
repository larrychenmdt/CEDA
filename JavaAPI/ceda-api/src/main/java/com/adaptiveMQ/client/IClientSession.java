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

import com.adaptiveMQ.message.BaseDestination;
import com.adaptiveMQ.message.IMessage;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.TemporaryDestination;

import java.util.List;

public interface IClientSession
{
    /**
     * 会话ID
     *
     * @param null
     * @return SessionID：会话ID
     * @throws
     */
    int getSessionID();

    /**
     * 批量订阅（MQ3新增）
     *
     * @param List<BaseDestination> destinationList： 地址队列,
     *                              IMessageListener listener：
     * @return void
     * @throws
     */
    void subscribe(List<BaseDestination> destinationList, IMessageListener listener);

    /**
     * 批量订阅（MQ3新增）
     *
     * @param List<BaseDestination> destinationList： 地址队列,
     *                              IMessageListener listener：
     * @return void
     * @throws
     */
    void subscribe(List<BaseDestination> destinationList,
                          String svrID,String signalID,IMessageListener listener) throws Exception;

    /**
     * 批量退订
     *
     * @param List<BaseDestination> destinationList： 地址队列,
     *                              IMessageListener listener：
     * @return void
     * @throws
     */
    void unSubscribe(List<BaseDestination> destinationList, IMessageListener listener);


    /**
     * 发送request消息（MQ3新增）
     *
     * @param Message msg： request消息, MessageRequestor request：消息请求器
     * @return void
     * @throws
     */
    Message sendRequest(Message msg, long timeoutMilli);

    /**
     * 发送request消息（MQ3新增）
     *
     * @param Message msg： request消息, IMessageListener listener：消息监听器
     * @return void
     * @throws
     */
    int sendRequest(Message msg, IMessageListener listener);

    /**
     * 发送消息
     *
     * @param Message msg
     * @return void
     * @throws
     */
    int send(IMessage msg);

    /**
     * 创建临时地址
     *
     * @param null
     * @return TemporaryDestination：临时地址
     * @throws
     */
    TemporaryDestination createTemporaryDestination(String str);
}

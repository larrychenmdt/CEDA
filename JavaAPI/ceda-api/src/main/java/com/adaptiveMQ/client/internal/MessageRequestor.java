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

package com.adaptiveMQ.client.internal;

import com.adaptiveMQ.client.ConnectionException;
import com.adaptiveMQ.client.IClientSession;
import com.adaptiveMQ.client.IMessageListener;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.TemporaryDestination;
import com.adaptiveMQ.utils.Consts;
import com.adaptiveMQ.utils.ConstsMessage;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MessageRequestor implements IMessageListener
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageRequestor.class);

    private IClientSession session;
    private TemporaryDestination temporaryDestination = null;
    private Message msgResult = null;        //返回的结果
    private CountDownLatch countDownLatch = null;

    /**
     * MessageRequestor构造
     *
     * @param IClientSession session：客户端 session;
     * @return MessageRequestor：请求消息类
     * @throws ConnectionException
     */
    public MessageRequestor(IClientSession session)
    {
        this.session = session;
    }

    /**
     * 请求回应消息
     *
     * @param Message msg： 请求的数据消息, long lWaitTime msg： 最多等待时间，单位毫秒
     * @return Message：返回的reply消息，超时则返回null；
     * @throws
     */
    public Message request(Message msg, long lWaitTime)
    {
        if (msg == null) {
            return null;
        }

        if (lWaitTime < 0) {
            return null;
        }

        msg.setInterMsgType(ConstsMessage.MSG_TYPE_DATA_REQ);
        msg.setDataType(Message.DATA_P2P);
        //默认发布到MQ
        if (msg.getSvrID() == null || msg.getSvrID().length() < 1) {
            msg.setSvrID("MQ");
        }
        msg.setP2PType(Message.P2P_TYPE_REQUEST);
        //设置消息的返回地址
        temporaryDestination = session.createTemporaryDestination(Consts.BlankStr);

        countDownLatch = new CountDownLatch(1);
        msg.setReplyTo(temporaryDestination);

        //_lock = new Object();
        //发送消息
        //_sender.send(msg);
        msgResult = null;
        session.sendRequest(msg, this);

        try {
            countDownLatch.await(lWaitTime, TimeUnit.MILLISECONDS);
        }
        catch (Exception e) {
            logger.error(e);
        }
        return msgResult;
    }

    /**
     * 响应数据消息，内部使用
     *
     * @param Message msg:数据消息
     * @return void；
     * @throws
     */
    public void onMessage(Message msg)
    {
        msgResult = msg;
        countDownLatch.countDown();
    }

    public void close()
    {
    }
}

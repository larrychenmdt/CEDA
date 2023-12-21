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

import com.adaptiveMQ.client.internal.CSubscribeImageHandler;
import com.adaptiveMQ.client.internal.MessageRequestor;
import com.adaptiveMQ.client.internal.MessageProcessor;
import com.adaptiveMQ.client.internal.MessageProcessorIn;
import com.adaptiveMQ.message.BaseDestination;
import com.adaptiveMQ.message.IMessage;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.TemporaryDestination;
import com.adaptiveMQ.utils.Consts;
import com.adaptiveMQ.utils.ConstsMessage;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientSession implements IClientSession
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClientSession.class);
    private final AtomicInteger tempID;
    private final CSubscribeImageHandler subImageHandler;
    private final MessageProcessorIn messageIn;
    private int connID;
    private int sessionID;
    private MessageProcessor msgProcessor;
    private CopyOnWriteArraySet<String> subscribeSet;

    public ClientSession(int connID, int sessionID, MessageProcessor msgProcessor)
    {
        this.connID = connID;
        this.sessionID = sessionID;
        this.msgProcessor = msgProcessor;
        messageIn = this.msgProcessor.getMessageProcessorIn();
        subImageHandler = new CSubscribeImageHandler(this.msgProcessor, this);
        tempID = new AtomicInteger(1);
        subscribeSet = new CopyOnWriteArraySet<>();
    }

    public TemporaryDestination createTemporaryDestination(String str)
    {
        return new TemporaryDestination(str, connID, sessionID, tempID.getAndIncrement());
    }

    public int getSessionID()
    {
        return sessionID;
    }

    public void subscribe(List<BaseDestination> destinationList,
                          String svrID,String signalID,IMessageListener listener) throws Exception
    {
        if(!msgProcessor.isLogin())
        {
            logger.error("failed to subscribe, not login");
            return;
        }

        if(destinationList==null ||svrID==null || signalID==null
                || listener==null || destinationList.size()==0)
        {
            logger.error("failed to subscribe, parameter fail");
            return;
        }

        Message msg=new Message();
        msg.getDestination().setName(ConstsMessage.TOPIC_API_REQUESTIMAGE);
        TemporaryDestination tdes=createTemporaryDestination(ConstsMessage.TOPIC_API_REQUESTIMAGE);
        msg.setReplyTo(tdes);
        msg.setSvrID(svrID);
        msg.setSignalID(signalID);
        msg.setInterMsgType(ConstsMessage.MSG_TYPE_DATA_REQ);
        msg.setP2PType(Message.P2P_TYPE_REQUEST);
        StringBuilder sbuff=new StringBuilder("[\"");
        int ncount=destinationList.size();
        int i=0;

        for( BaseDestination des : destinationList)
        {
            ++i;
            sbuff.append(des.getName());
            if(i<ncount)
            {
                sbuff.append("\",\"");
            }else{
                sbuff.append("\"");
            }
        }
        sbuff.append("]");
        String svalue=sbuff.toString();

        msg.getMessageBody().addInt((short)5,0);
        msg.getMessageBody().addString((short)3, svalue);
        subImageHandler.addRequestTopic(destinationList, tdes.getName(), listener);
        send(msg);
    }

    public void subscribe(List<BaseDestination> destinationList, IMessageListener listener)
    {
        if (destinationList == null || listener == null || destinationList.size() < 1) {
            logger.error("subscribe parameter error");
            return;
        }
        boolean isOk = true;

        //单次订阅，如果已经订阅过，返回
        if (destinationList.size() == 1) {
            // 已经订阅过，返回,防止consumber重新订阅
            if (subscribeSet.contains(destinationList.get(0).getName())) {
                listenerAdded(destinationList.get(0), listener);
                return;
            }
        }
        //MQ的服务，从MQ服务器订阅；
        try {
            msgProcessor.subscribe(destinationList);
        }
        catch (Exception e) {
            logger.error(e);
            isOk = false;
        }

        if (isOk) {
            try {
                for (int i = 0; i < destinationList.size(); i++) {
                    BaseDestination dest = destinationList.get(i);
                    subscribeSet.add(dest.getName());
                    listenerAdded(dest, listener);
                }
                logger.debug("ClientSession: subscribe(%s)", destinationList);
            }
            catch (Exception e) {
                logger.error(e);
            }
        }
    }

    public void registerMessageListener(Collection<BaseDestination> destinationList, IMessageListener listener)
    {
        try {
            for (BaseDestination dest : destinationList) {
                subscribeSet.add(dest.getName());
                logger.debug("ClientSession: register(%s)", dest);
                listenerAdded(dest, listener);
            }
        }
        catch (Exception e) {
            logger.error(e);
        }
    }

    public void unSubscribe(List<BaseDestination> destinationList, IMessageListener listener)
    {
        if (destinationList == null || listener == null || destinationList.size() < 1) {
            logger.error("unSubscribe parameter fail");
            return;
        }

        try {
            msgProcessor.unsubscribe(destinationList);
        }
        catch (Exception e) {
            logger.error("unsubscribe failed", e);
            return;
        }

        for (BaseDestination dest : destinationList) {
            subscribeSet.remove(dest.getName());
            listenerRemove(dest, listener);
        }

        logger.debug("ClientSession: unsubscribe(%s)", destinationList);
    }

    public Message sendRequest(Message msg, long timeoutMilli)
    {
        if (!msgProcessor.isLogin()) {
            logger.error("not login");
            return null;
        }
        if (msg == null) {
            logger.error("Message is NULL");
            return null;
        }

        MessageRequestor requestor = new MessageRequestor(this);
        return requestor.request(msg, timeoutMilli);
    }

    public int sendRequest(Message msg, IMessageListener listener)
    {
        if (!msgProcessor.isLogin()) {
            logger.error("not login");
            return 0;
        }
        if (msg == null || msg.getDestination() == null) {
            logger.error("Message or topic is NULL");
            return 0;
        }

        msg.setReplyTo(createTemporaryDestination(Consts.BlankStr));
        msg.setInterMsgType(ConstsMessage.MSG_TYPE_DATA_REQ);
        //msg.setDataType(Message.DATA_P2P);
        //msg.setP2PType(Message.P2P_TYPE_REQUEST);
        return msgProcessor.sendMessage(msg, listener);
    }

    public int send(IMessage msg)
    {
        if (msg == null) {
            logger.error("Message is NULL");
            return -1;
        }
        if (!msgProcessor.isLogin()) {
            logger.error("not login");
            return 0;
        }
        return msgProcessor.sendMessage(msg);
    }

    private void listenerRemove(BaseDestination desc, IMessageListener listener)
    {
        messageIn.removeListener(desc, listener, false);
    }

    private void listenerAdded(BaseDestination desc, IMessageListener listener)
    {
        msgProcessor.getCachedMessage(desc, listener);
        messageIn.registerListener(desc, listener);
    }
}

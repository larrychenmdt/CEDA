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

import com.adaptiveMQ.client.IMessageListener;
import com.adaptiveMQ.message.BaseDestination;
import com.adaptiveMQ.message.ControlMessage;
import com.adaptiveMQ.message.IMessage;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.utils.ConstsMessage;
import com.adaptiveMQ.utils.Utils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public final class MessageProcessorIn
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageProcessorIn.class);
    private final HashSet<IMessageListener> messageListeners;

    //保存普通订阅消息的Session
    private ConcurrentHashMap<String, CopyOnWriteArraySet<IMessageListener>> hashSessions = null;

    //保存通配符订阅消息的Session
    private ConcurrentHashMap<String, CopyOnWriteArraySet<IMessageListener>> hashWildSessions = null;

    //保存接收到的消息topic 匹配 订阅通配符
    private ConcurrentHashMap<String, CopyOnWriteArraySet<String>> receTopicMatch = null;

    private MessageProcessor mgr = null;
    private Vector<Message> messageCache = null;            //保存接收到的,但是没有发送到监听器的消息
    private MessageCacheList messageCacheList = null;    //保存消息的CacheList
    private CRequestHandler requestHandler;
    private IMessageListener loginListener = null;
    private CSubscribeImageHandler subImageHandler;

    public MessageProcessorIn(MessageProcessor processor)
    {
        hashSessions = new ConcurrentHashMap<String, CopyOnWriteArraySet<IMessageListener>>();
        hashWildSessions = new ConcurrentHashMap<String, CopyOnWriteArraySet<IMessageListener>>();
        receTopicMatch = new ConcurrentHashMap<String, CopyOnWriteArraySet<String>>();
        messageCache = new Vector<Message>();
        messageCacheList = new MessageCacheList();
        mgr = processor;
        requestHandler = processor.getRequestHandler();
        messageListeners = new HashSet<IMessageListener>();
    }

    public void setSubImageHander(CSubscribeImageHandler subImageHandler)
    {
        this.subImageHandler = subImageHandler;
    }

    void setLoginListener(IMessageListener msgListener)
    {
        loginListener = msgListener;
    }

    private void processControl(IMessage msg)
    {
        ControlMessage ctlMsg = (ControlMessage) msg;
        byte ntype = ctlMsg.getInterMsgType();
        switch (ntype) {
            case ConstsMessage.MSG_TYPE_CTRL_HB:
                break;
            case ConstsMessage.MSG_TYPE_CTRL_LOGIN:
                mgr.checkUserResult(ctlMsg.getMessageBody());
                break;
            case ConstsMessage.MSG_TYPE_CTRL_LOGOUT: {
                logger.info("MessageProcessorIn: Received a logout message");
                mgr.close();
            }
            break;
            default:
                logger.error("unsport type control message, type:" + ntype);
        }
    }

    public void addMessage(IMessage msg)
    {
        byte nType = msg.getInterMsgType();
        if (nType < ConstsMessage.MSG_TYPE_DATA_PUB) {
            processControl(msg);
        }
        else {
            Message dataMsg = (Message) msg;
            //如果是登录消息，使用LoginListener监听
            String stopic = dataMsg.getDestination().getName();
            if (stopic.equals(ConstsMessage.TOPIC_ATS_LOGIN)) {
                if (loginListener != null) {
                    loginListener.onMessage(dataMsg);
                }
                return;
            }
            else if (nType == ConstsMessage.MSG_TYPE_DATA_REPLY) {
                logger.debug("MessageProcessorIn: Received a reply message");
                if (stopic.indexOf(ConstsMessage.TOPIC_API_REQUESTIMAGE) > -1 || stopic.indexOf(ConstsMessage.TOPIC_ACS_REQUESTIMAGE) > -1) {
                    subImageHandler.onMessage(dataMsg, stopic);
                    dataMsg = null;
                    return;
                }
                else {
                    if (requestHandler.onMessage(dataMsg, stopic)) {
                        return;
                    }
                }
            }

            //从普通订阅中获取session
            CopyOnWriteArraySet<IMessageListener> vec1 = hashSessions.get(stopic);
            if (vec1 != null) {
                issueMessage(dataMsg, vec1);
            }

            //获取match的通配符订阅
            CopyOnWriteArraySet<String> vecWild = receTopicMatch.get(stopic);
            if (vecWild == null) {
                //第一次收到该消息
                vecWild = new CopyOnWriteArraySet<String>();
                Enumeration<String> enumWildcard = hashWildSessions.keys();
                while (enumWildcard.hasMoreElements()) {
                    String szWildDesc = enumWildcard.nextElement();
                    if (Utils.matchDestination(szWildDesc, stopic)) {
                        vecWild.add(szWildDesc);
                    }
                }
                receTopicMatch.put(stopic, vecWild);
            }

            for (String wTopic : vecWild) {
                CopyOnWriteArraySet<IMessageListener> vec2 = hashWildSessions.get(wTopic);
                issueMessage(dataMsg, vec2);
            }
            messageListeners.clear();
        }
    }

    private void issueMessage(Message dataMsg, CopyOnWriteArraySet<IMessageListener> vec)
    {
        if (vec == null) {
            return;
        }
        for (IMessageListener listener : vec) {
            if (listener != null) {
                if (!messageListeners.contains(listener)) {
                    messageListeners.add(listener);
                    listener.onMessage(dataMsg);
                }
            }
        }
    }

    //注册一个用户的session和订阅地址
    public void registerListener(BaseDestination destination, IMessageListener listen)
    {
        CopyOnWriteArraySet<IMessageListener> sessionList = null;
        Boolean isWild = false;
        if (Utils.isWildcard(destination.getName())) {
            sessionList = hashWildSessions.get(destination.getName());
            isWild = true;
        }
        else {
            sessionList = hashSessions.get(destination.getName());
        }

        if (sessionList == null) {
            sessionList = new CopyOnWriteArraySet<IMessageListener>();
            if (isWild) {
                hashWildSessions.put(destination.getName(), sessionList);
                //如果匹配接收topic，添加到接收topic路径中；
                Enumeration<String> enuTopic = receTopicMatch.keys();
                while (enuTopic.hasMoreElements()) {
                    String stopic = enuTopic.nextElement();
                    if (Utils.matchDestination(destination.getName(), stopic)) {
                        CopyOnWriteArraySet<String> vec = receTopicMatch.get(stopic);
                        if (vec != null) {
                            vec.add(destination.getName());
                        }
                    }
                }
            }
            else {
                hashSessions.put(destination.getName(), sessionList);
            }
        }
        //将监听器增加到列表
        sessionList.add(listen);
    }

    //去掉一个用户的session和订阅地址
    public void removeListener(BaseDestination destination, IMessageListener listener, boolean isPersistance)
    {
        CopyOnWriteArraySet<IMessageListener> sessionList = null;
        if (Utils.isWildcard(destination.getName())) {
            if (listener == null) {
                hashWildSessions.remove(destination.getName());
            }
            else {
                sessionList = hashWildSessions.get(destination.getName());
            }

        }
        else {
            if (listener == null) {
                hashSessions.remove(destination.getName());
            }
            else {
                sessionList = hashSessions.get(destination.getName());
            }

        }

        if (sessionList != null) {
            //将监听器增加到列表
            sessionList.remove(listener);
        }
    }

    //把Cache的消息发送到监听器
    public void getCachedMessage(IMessageListener listener)
    {
        Message msg = null;
        if (listener != null) { //发送给监听器
            int size = messageCache.size();
            for (int i = 0; i < size; i++) {
                msg = messageCache.elementAt(0);
                messageCache.removeElementAt(0);
                listener.onMessage(msg);
            }
        }
    }

    //把CacheList的消息发送到监听器
    public void getCachedList(BaseDestination desc, IMessageListener listener)
    {
        Message msg = null;
        if (listener != null) {
            Vector<Message> cacheListMessageList = messageCacheList.getMessageList(desc);
            for (int i = 0; cacheListMessageList != null && i < cacheListMessageList.size(); i++) {
                msg = cacheListMessageList.elementAt(i);
                listener.onMessage(msg);
            }
        }
    }
}

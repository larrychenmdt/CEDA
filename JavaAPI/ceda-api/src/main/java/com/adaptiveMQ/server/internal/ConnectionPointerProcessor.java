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

package com.adaptiveMQ.server.internal;

import com.adaptiveMQ.client.internal.IMessageProcessor;
import com.adaptiveMQ.message.BaseDestination;
import com.adaptiveMQ.message.ControlMessage;
import com.adaptiveMQ.message.IMessage;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.MessageBody;
import com.adaptiveMQ.message.MessageUtils;
import com.adaptiveMQ.server.IServerLoginHandler;
import com.adaptiveMQ.server.IServerConnectionListener;
import com.adaptiveMQ.server.IServerConnectionMessageListener;
import com.adaptiveMQ.server.IServerSubscriptionHandler;
import com.adaptiveMQ.utils.ConstsMessage;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;

public final class ConnectionPointerProcessor implements IMessageProcessor
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnectionPointerProcessor.class);

    private final ConnectionPoint connectionPoint;
    private final int connectionID;
    private IServerConnectionListener serverConnectionEventListener = null;
    private IServerConnectionMessageListener serverMessageHandler = null;
    private IServerLoginHandler loginHandler = null;
    private IServerSubscriptionHandler subscriptionHandler = null;
    private ConnectionPointManager connectionPointManager = null;

    public ConnectionPointerProcessor(ConnectionPoint handler, ConnectionPointManager handlerManage)
    {
        connectionPoint = handler;
        connectionPointManager = handlerManage;
        connectionID = connectionPointManager.generateNextConntionID();
        connectionPoint.setConnectionID(connectionID);
    }

    void setConnectionListener(IServerConnectionListener listener)
    {
        serverConnectionEventListener = listener;
    }

    void setLoginHandler(IServerLoginHandler serverConnectionAuth)
    {
        this.loginHandler = serverConnectionAuth;
    }

    void setMessageHandler(IServerConnectionMessageListener msgListener)
    {
        serverMessageHandler = msgListener;
    }

    void setSubscriptionHandler(IServerSubscriptionHandler subscriptionHandler)
    {
        this.subscriptionHandler = subscriptionHandler;
    }

    void close()
    {
        serverMessageHandler = null;
        //m_handlerManage.disConnect(m_handler);
    }

    private void outMsg(IMessage msg)
    {
        connectionPoint.sendMessage(msg);
    }

    private void dealSubscribe(byte btype, MessageBody body)
    {
        ArrayList<BaseDestination> topicList = new ArrayList<BaseDestination>();

        try {
            if (btype == ConstsMessage.CTRL_CODE_SUB || btype == ConstsMessage.CTRL_CODE_UNSUB) {
                String stopic = body.getString((short) 3);
                BaseDestination des = new BaseDestination(stopic);
                topicList.add(des);

            }
            else {
                String sline = body.getString((short) 3);
                String[] topicArr = sline.split("\\|");
                for (String stopic : topicArr) {
                    if (stopic != null) {
                        BaseDestination des = new BaseDestination(stopic);
                        //des.setType(bType);
                        topicList.add(des);
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error("parse destination fail:", e);
        }

        if (serverMessageHandler != null) {
            if (topicList.size() > 0) {
                if (btype == ConstsMessage.CTRL_CODE_SUB || btype == ConstsMessage.CTRL_CODE_SUB_BATCH) {
                    subscriptionHandler.onSubscribe(topicList, connectionPoint, true);
                }
                else {
                    subscriptionHandler.onSubscribe(topicList, connectionPoint, false);
                }
            }
        }
    }

    private void dealLongin(boolean isLoginMsg, MessageBody body)
    {
        boolean isLogin = false;
        String username = "";

        if (isLoginMsg) {
            Message dmsg = new Message();
            dmsg.getDestination().setName(ConstsMessage.TOPIC_API_LOGIN);
            MessageUtils.copyRecord(body, dmsg.getMessageBody());
            connectionPoint.getClientInfo().setLoginMessage(dmsg);
        }
        else {
            String pass = "";
            try {
                username = body.getString((short) 4);
                pass = body.getString((short) 5);

            }
            catch (Exception e) {
                logger.error("get username and pass failed", e);
            }
            connectionPoint.getClientInfo().setUser(username, pass);
        }

        try {
            isLogin = loginHandler.onUserLogin(connectionPoint.getClientInfo(), connectionPoint);
        }
        catch (Exception e) {
            logger.error("userValidation failed", e);
            e.printStackTrace();
        }
        outLoginResult(isLogin, username);
    }

    private void outLoginResult(boolean isLogin, String username)
    {
        ControlMessage ctlMsg = new ControlMessage();
        ctlMsg.setInterMsgType(ConstsMessage.MSG_TYPE_CTRL_LOGIN);
        ctlMsg.setControlCode(ConstsMessage.CTRL_CODE_LOGIN_RESULT);

        MessageBody body = ctlMsg.getMessageBody();
        String sRet = "";

        try {
            if (isLogin) {
                connectionPointManager.loginSuccess(connectionPoint);
                //成功
                sRet = "Success";
                body.addInt((short) 1, 1);
                body.addInt((short) 6, connectionID);

            }
            else {
                connectionPointManager.loginFail(username);
                sRet = "Username or Password is error";
                body.addInt((short) 1, 0);
            }

            body.addString((short) 4, sRet);
            outMsg(ctlMsg);

        }
        catch (Exception e) {
            logger.error("outLoginResult failed", e);
        }
    }

    private void dealPing()
    {
        ControlMessage ctlMsg = new ControlMessage();
        ctlMsg.setInterMsgType(ConstsMessage.MSG_TYPE_CTRL_HB);
        outMsg(ctlMsg);
    }

    private void processControl(IMessage msg)
    {
        ControlMessage ctlMsg = (ControlMessage) msg;
        byte bType = ctlMsg.getInterMsgType();
        byte controlCode = ctlMsg.getControlCode();
        switch (bType) {
            case ConstsMessage.MSG_TYPE_CTRL_LOGIN:                //服务器端回应的heart消息
            {
                dealLongin(controlCode == ConstsMessage.CTRL_CODE_LOGIN_VS, ctlMsg.getMessageBody());
                break;
            }
            case ConstsMessage.MSG_TYPE_CTRL_HB:                //服务器端回应的heart消息
            {
                dealPing();
                break;
            }
            case ConstsMessage.MSG_TYPE_CTRL_LOGOUT:                //服务器端回应的heart消息
            {
                //dealPing();
                break;
            }
            case ConstsMessage.MSG_TYPE_CTRL_SUB:            //接收到服务器端发送给客户端的Ping结果
                //_mgr.getPingResult();
                switch (controlCode) {
                    case ConstsMessage.CTRL_CODE_SUB: {
                        dealSubscribe(ConstsMessage.CTRL_CODE_SUB, ctlMsg.getMessageBody());
                        break;
                    }
                    case ConstsMessage.CTRL_CODE_SUB_BATCH:    //批量订阅
                    {
                        //_mgr.subscribeResult(ctlMsg.getCommand());
                        dealSubscribe(ConstsMessage.CTRL_CODE_SUB_BATCH, ctlMsg.getMessageBody());
                        break;
                    }
                }

                break;
            case ConstsMessage.MSG_TYPE_CTRL_UNSUB:            //接收到服务器端发送给客户端的Ping结果
                //_mgr.getPingResult();
                switch (controlCode) {
                    case ConstsMessage.CTRL_CODE_UNSUB: {
                        dealSubscribe(ConstsMessage.CTRL_CODE_UNSUB, ctlMsg.getMessageBody());
                        break;
                    }
                    case ConstsMessage.CTRL_CODE_UNSUB_BATCH:    //批量订阅
                    {
                        //_mgr.subscribeResult(ctlMsg.getCommand());
                        dealSubscribe(ConstsMessage.CTRL_CODE_UNSUB_BATCH, ctlMsg.getMessageBody());
                        break;
                    }
                }

                break;
            default:
                logger.warn(String.format("unsport control message, message type[%d]", bType));
        }
    }

    public void addInMessage(IMessage msg)
    {
        byte ntype = msg.getInterMsgType();
        if (ntype < ConstsMessage.MSG_TYPE_DATA_PUB) {
            processControl(msg);
        }
        else {
            Message dmsg = (Message) msg;
            //服务器间同步消息
            if (dmsg.getDataType() == Message.DATA_SYN) {
                //dmsg.setSignalID(m_handler.getClientInfo().getUsername());
                connectionPointManager.onSyncMessage(dmsg, connectionPoint);
            }
            else if (dmsg.getDestination().getName().equals(ConstsMessage.TOPIC_API_LOGIN)) {
                //API使用登录消息，直接登录
                connectionPoint.getClientInfo().setLoginMessage(dmsg);
                boolean isLogin = loginHandler.onUserLogin(connectionPoint.getClientInfo(), connectionPoint);
                outLoginResult(isLogin, "");
            }
            else {
                if (serverMessageHandler != null) {
                    serverMessageHandler.onMessage(dmsg, connectionPoint);
                }
            }
        }
    }
}

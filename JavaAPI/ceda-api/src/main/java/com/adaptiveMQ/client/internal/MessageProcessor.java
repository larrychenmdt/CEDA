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

import com.adaptiveMQ.client.ClientInfo;
import com.adaptiveMQ.client.ConnectionException;
import com.adaptiveMQ.client.IClientConnection;
import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.client.IMessageListener;
import com.adaptiveMQ.client.transport.IClientWriteService;
import com.adaptiveMQ.message.BaseDestination;
import com.adaptiveMQ.message.ControlMessage;
import com.adaptiveMQ.message.IMessage;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.MessageBody;
import com.adaptiveMQ.message.MessageUtils;
import com.adaptiveMQ.server.internal.MessageExchanger;
import com.adaptiveMQ.utils.ConstsMessage;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.ArrayList;
import java.util.List;

public final class MessageProcessor implements IMessageProcessor
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageExchanger.class);

    private final Object lockCheckUser = new Object();
    protected int pingInterval = 15;            //默认Ping为15秒
    protected int heartbeatInterval = 15;        //默认15秒发送一次心跳
    private ClientInfo clientInfo;
    private MessageProcessorIn messageProcessorIn;
    private MessageProcessorOut messageProcessorOut;
    private IClientWriteService clientWrite = null;
    private IClientConnection clientConnection;
    private MessageBody checkUserResult = null;
    private CRequestHandler requestHandler;

    private boolean isLogin = false;

    public MessageProcessor(IClientConnection conn, ClientInfo oClientInfo)
    {
        clientInfo = oClientInfo;
        requestHandler = new CRequestHandler(this);
        messageProcessorIn = new MessageProcessorIn(this);
        messageProcessorOut = new MessageProcessorOut(this);
        clientConnection = conn;
    }

    public boolean isLogin()
    {
        return isLogin;
    }

    public MessageProcessorIn getMessageProcessorIn()
    {
        return messageProcessorIn;
    }

    public void close()
    {
        clientConnection.close();
    }

    public CRequestHandler getRequestHandler()
    {
        return requestHandler;
    }

    //设置Ping的时间间隔
    public void setPingInterval(int interval)
    {
        pingInterval = interval;
        if (pingInterval < 0) {
            pingInterval = 0;
        }
    }

    //设置Heartbeat的时间间隔
    public void setHeartbeatInterval(int interval)
    {
        heartbeatInterval = interval;
        if (heartbeatInterval < 0) {
            heartbeatInterval = 15;
        }
    }

    //将一个从Reader接收到的消息，放入MessageProcessorIn处理器去处理
    public void addInMessage(IMessage msg)
    {
        clientWrite.resetPing();    //收到了服务器发送过来的信息，说明没有断线，重新计算Ping的时间
        messageProcessorIn.addMessage(msg);
    }

    //发送一个事件到ClientConnection
    public void sendEvent(int nCode)
    {
        switch (nCode) {
            case IEventListener.CONNECTION_LOST:
                isLogin = false;
                break;
            case IEventListener.CONNECTION_CLOSED:
                isLogin = false;
                //_oClientConnection.close();
                break;

        }
        //System.out.println("*** sendEvent:"+nCode);
        clientConnection.sendEvent(nCode);
    }

    public void setLoginListener(IMessageListener msgListener)
    {
        messageProcessorIn.setLoginListener(msgListener);
    }

    public int sendMessage(Message dmsg, MessageRequestor request)
    {
        if (setP2PSignal(dmsg) == -1) {
            return -1;
        }

        requestHandler.addRequest(dmsg, request);
        messageProcessorOut.addMessage(dmsg);
        return 1;
    }

    public int sendMessage(Message dmsg, IMessageListener listener)
    {
        if (setP2PSignal(dmsg) == -1) {
            return -1;
        }

        requestHandler.addRequest(dmsg, listener);
        messageProcessorOut.addMessage(dmsg);
        return 1;
    }

    private int setP2PSignal(Message dmsg)
    {
        boolean isOK = true;
        if (dmsg.getSvrID() == null || dmsg.getDestination() == null) {
            isOK = false;
        }

        if (isOK) {
            if (dmsg.getInterMsgType() == ConstsMessage.MSG_TYPE_DATA_REQ) {
                if (dmsg.getReplyTo() == null) {
                    isOK = false;
                }
            }
        }

        if (!isOK) {
            logger.error("p2p message must set SvrID, Destination, Reply");
            return -1;
        }

        //dmsg.setACSID(m_clientInfo.getACSID());
        //dmsg.setUserID(_oClientInfo.getUsername());
        if (dmsg.getConnectionID() == -1) {
            dmsg.setConnectionID(clientInfo.getLoginConnID());
        }
        if (dmsg.getInterMsgType() == ConstsMessage.MSG_TYPE_DATA_PUB) {
            dmsg.setInterMsgType(ConstsMessage.MSG_TYPE_DATA_P2P);
        }
        return 0;
    }

    //发送一条消息到服务器
    public int sendMessage(IMessage msg)
    {
        //int iMessageID = 0;
        if (!isLogin) {
            logger.error("MessageProcessor: sendMessage must login");
            return -1;
        }

        if (msg.getInterMsgType() > 90) {
            Message dmsg = (Message) msg;
            if (dmsg.getDataType() == Message.DATA_P2P) {
                if (setP2PSignal(dmsg) == -1) {
                    return -1;
                }
            }
        }

        //增加一条待发送信息
        messageProcessorOut.addMessage(msg);
        return 1;
    }

    //重新发送一条消息到服务器
    public void resendMessage(IMessage msg)
    {
        messageProcessorOut.addMessage(msg);
        logger.debug("MessageProcessor: Resend a message, message type " + IMessage.MESSAGE_TYPE[msg.getMessageType()]);
    }

    //设置数据发送器
    public void setWriter(IClientWriteService writer)
    {
        clientWrite = writer;
        messageProcessorOut.setWriter(clientWrite);
        clientWrite.setPingInterval(pingInterval);
        clientWrite.setHeartbeatInterval(heartbeatInterval);
    }

    //验证用户名密码
    protected void checkUser(ClientInfo cInfo, String strClientID) throws ConnectionException
    {
        ControlMessage ctlMsg = null;
        String sid = cInfo.getSID();

        if (sid != null && sid.length() > 2) {
            ctlMsg = MessageUtils.getSIDLoginInfo(sid, cInfo.getLoginConnID());
            messageProcessorOut.addMessage(ctlMsg);
        }
        else if (cInfo.getLoginMessage() != null) {
            Message msg = cInfo.getLoginMessage();
            messageProcessorOut.addMessage(MessageUtils.getVsLogin(msg));
        }
        else {
            ctlMsg = MessageUtils.getUserInfo(cInfo.getUsername(), cInfo.getPassword(), strClientID, cInfo.getLoginConnID());
            messageProcessorOut.addMessage(ctlMsg);
        }

        synchronized (lockCheckUser) {
            //_oProcessorOut.addMessage(ctlMsg);

            int nRetryTime = 0;
            try {
                while (checkUserResult == null) {
                    lockCheckUser.wait(100);

                    //如果15秒钟都连接不上，则超时
                    if (nRetryTime == 150) {
                        sendEvent(IEventListener.CONNECTION_TIMEOUT);
                        sendEvent(IEventListener.CONNECTION_CLOSED);
                        break;
                    }
                    nRetryTime++;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.error(e);
            }
        }

        if (checkUserResult == null) {
            sendEvent(IEventListener.CONNECTION_CLOSED);
            throw new ConnectionException("no reply");
            //return;
        }

        MessageUtils.LoginResult result = MessageUtils.parseLoginResult(checkUserResult);
        if (result.nCode == 1) {
            //保存LoginConnid
            clientInfo.setLoginConnID(result.nConnID);
            clientConnection.setConnectionID(result.nConnID);
            isLogin = true;
            //subscribeServiceInfo();
            logger.debug("MessageProcessor checkUser: Login success");
        }
        else {
            //需要loginmsg登陆或其它错误
            sendEvent(IEventListener.CONNECTION_CLOSED);
            throw new ConnectionException(result.szCmdString);
        }
    }

    //返回验证用户名密码结果
    protected void checkUserResult(MessageBody body)
    {
        logger.debug("MessageProcessor checkUserResult: Received a checkUser result message");

        synchronized (lockCheckUser) {
            try {
                checkUserResult = body;
                lockCheckUser.notify();
            }
            catch (Exception e) {
                logger.error(e);
            }
        }
    }

    //进行登陆
    public void login(ClientInfo cInfo, String strClientID) throws ConnectionException
    {
        //用户登陆
        checkUser(cInfo, strClientID);
    }

    //批量退订
    public void unsubscribe(List<BaseDestination> destinationList) throws ConnectionException
    {
        if (!isLogin) {
            //sendEvent(IEventListener.ERR_PUBLISH_LOGIN);
            return;
            //throw new ConnectionException("Cannot unsubscribe before login");
        }

        ControlMessage ctlMsg = MessageUtils.getBatchDestination(destinationList, ConstsMessage.MSG_TYPE_CTRL_UNSUB, ConstsMessage.CTRL_CODE_UNSUB_BATCH);
        //CLog.printDebug(CLog.LEVEL_HIGH,"MessageProcessor unsubscribe: Send a batch unsubscribe message");
        if (ctlMsg == null) {
            return;
        }
        messageProcessorOut.addMessage(ctlMsg);
    }

    //批量订阅
    public void subscribe(List<BaseDestination> destinationList) throws ConnectionException
    {
        if (!isLogin) {
            sendEvent(IEventListener.ERR_PUBLISH_LOGIN);
            throw new ConnectionException("Cannot subscribe before login");
        }
        ControlMessage ctlMsg = MessageUtils.getBatchDestination(destinationList, ConstsMessage.MSG_TYPE_CTRL_SUB, ConstsMessage.CTRL_CODE_SUB_BATCH);
        if (ctlMsg == null) {
            return;
        }

        //CLog.debug("subscribe: %s",destinationList);
        subscribe(ctlMsg);
    }

    private synchronized void subscribe(ControlMessage msg) throws ConnectionException
    {
        //订阅不再阻塞
        if (isLogin) {
            messageProcessorOut.addMessage(msg);
        }
        else {
            //没有建立连接就要订阅
            sendEvent(IEventListener.ERR_PUBLISH_LOGIN);
            throw new ConnectionException("Cannot subscribe before connected");
        }
    }

    //订阅消息
    public void subscribe(BaseDestination desc, boolean isPersistance) throws ConnectionException
    {
        if (!isLogin) {
            sendEvent(IEventListener.ERR_PUBLISH_LOGIN);
            throw new ConnectionException("Cannot subscribe before login");
        }

        List<BaseDestination> desList = new ArrayList<BaseDestination>();
        desList.add(desc);
        ControlMessage ctlMsg = MessageUtils.getBatchDestination(desList, ConstsMessage.MSG_TYPE_CTRL_SUB, ConstsMessage.CTRL_CODE_SUB_BATCH);
        //CLog.printDebug(CLog.LEVEL_HIGH,"MessageProcessor subscribe: Send a subscribe message");

        //CLog.debug("subscribe: %s",desc.getName());
        subscribe(ctlMsg);
    }

    //取消订阅消息
    public void unsubscribe(BaseDestination desc, boolean isPersistance) throws ConnectionException
    {
        if (!isLogin()) {
            return;
        }
        ControlMessage ctlMsg;
        List<BaseDestination> desList = new ArrayList<BaseDestination>();
        desList.add(desc);
        ctlMsg = MessageUtils.getBatchDestination(desList, ConstsMessage.MSG_TYPE_CTRL_UNSUB, ConstsMessage.CTRL_CODE_UNSUB_BATCH);
        //CLog.printDebug(CLog.LEVEL_HIGH,"MessageProcessor unsubscribe: Send an unsubscribe message");
        if (ctlMsg == null) {
            return;
        }

        if (messageProcessorOut.addMessage(ctlMsg) == 0) {
            //没有建立连接就要订阅
            throw new ConnectionException("Cannot subscribe before connected");
        }
    }

    //把Cache的消息发送给监听器
    public void getCachedMessage(BaseDestination desc, IMessageListener listener)
    {
        //把CacheList里面的消息给客户
        messageProcessorIn.getCachedList(desc, listener);

        //把连接后还没有发送给客户的Cache消息给客户
        messageProcessorIn.getCachedMessage(listener);
    }
}

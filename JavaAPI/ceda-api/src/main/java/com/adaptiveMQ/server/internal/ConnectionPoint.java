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

import com.adaptiveMQ.client.ClientInfo;
import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.message.ControlMessage;
import com.adaptiveMQ.message.IMessage;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.internal.MessageConverter;
import com.adaptiveMQ.server.IServerConnection;
import com.adaptiveMQ.server.IServerLoginHandler;
import com.adaptiveMQ.server.IServerConnectionListener;
import com.adaptiveMQ.server.IServerConnectionMessageListener;
import com.adaptiveMQ.server.IServerSubscriptionHandler;
import com.adaptiveMQ.server.transport.IChannel;
import com.adaptiveMQ.utils.BufferByte;
import com.adaptiveMQ.utils.ConstsMessage;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Calendar;

public final class ConnectionPoint implements IServerConnection
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnectionPoint.class);

    private final IServerConnectionListener handlerLiestener;
    private final IServerLoginHandler serverConnectionAuth;
    private final ClientInfo clientInfo;
    private final ConnectionPointManager handlerManage;
    private final String inFormat = "Client[%d] receive bytes[%d]";
    private final String mFormat = "Client[%d] send bytes[%d]";
    private boolean logout = false;
    private RecvDataBuffer recvDataBuffer = null;        //缓冲接收到的数据
    private long lastTime;

    private int pingCount;
    private String socketAddress = null;
    private int nConnID;
    private IChannel channel = null;
    private BufferByte bodyBuf;
    private BufferByte msgBuf;

    public ConnectionPoint(String address, IChannel channel, IServerConnectionListener listener, IServerLoginHandler serverConnectionAuth, ConnectionPointManager handlerManage)
    {
        socketAddress = address.substring(1);
        handlerLiestener = listener;
        this.serverConnectionAuth = serverConnectionAuth;
        nConnID = -1;

        this.handlerManage = handlerManage;
        recvDataBuffer = new RecvDataBuffer(this, handlerLiestener, serverConnectionAuth);

        this.channel = channel;
        msgBuf = new BufferByte();
        bodyBuf = new BufferByte();

        clientInfo = new ClientInfo();
    }

    @Override
    public void setMessageHandler(IServerConnectionMessageListener msgListener)
    {
        recvDataBuffer.setMessageHandler(msgListener);
    }

    @Override
    public void setSubscriptionHandler(IServerSubscriptionHandler subscriptionHandler)
    {
        recvDataBuffer.setSubscriptionHandler(subscriptionHandler);
    }

    ConnectionPointManager getHandlerManage()
    {
        return handlerManage;
    }

    long getActiveTime()
    {
        return lastTime;
    }

    int getPingTimes()
    {
        return pingCount;
    }

    public int getConnectionID()
    {
        return nConnID;
    }

    void setConnectionID(int connid)
    {
        nConnID = connid;
    }

    public String getSocketAddress()
    {
        return socketAddress;
    }

    private void resetLastActiveTime()
    {
        lastTime = Calendar.getInstance().getTimeInMillis();
    }

    private void resetPingTimes()
    {
        pingCount = 0;
    }

    void ping()
    {
        ControlMessage ctlMsg = new ControlMessage();
        ctlMsg.setInterMsgType(ConstsMessage.MSG_TYPE_CTRL_HB);
        ctlMsg.setControlCode(ConstsMessage.MSG_TYPE_CTRL_HB);
        //ctlMsg.setCommand(" ");
        sendMessage(ctlMsg);
        pingCount++;
        resetLastActiveTime();
    }

    public void disConnect()
    {
        try {
            if (!logout) {
                handlerManage.logout(this);
            }
            logout = true;
            handlerLiestener.onEvent(IEventListener.CONNECTION_CLOSED, this);

        }
        catch (Exception e) {
            logger.error(e);
        }
        if (msgBuf != null) {
            msgBuf.close();
            msgBuf = null;
        }

        if (bodyBuf != null) {
            bodyBuf.close();
            bodyBuf = null;

        }
        if (recvDataBuffer != null) {
            recvDataBuffer.close();
            recvDataBuffer = null;
        }
    }

    public void closePhysicalConnect()
    {
        channel.close();
    }

    void loginSuccess()
    {
        logger.info("Client[" + nConnID + "] login, user:" + clientInfo.getUsername());
        try {
            handlerLiestener.onEvent(IEventListener.CONNECTION_LOGIN_SUCCESS, this);
            //m_login=true;
        }
        catch (Exception e) {
            logger.error("failed to process loginSuccess event", e);
        }
    }

    public void inPutData(byte[] buffer, int iRecSize)
    {
        resetLastActiveTime();
        resetPingTimes();
        logger.debug(String.format(inFormat, nConnID, iRecSize));
        recvDataBuffer.addData(buffer);
    }

    private void outPut(IMessage msg)
    {
        try {
            byte[] bstr = msg.getWsStream();

            // 存在字符，则发送；
            if (bstr == null) {
                byte[] bstream = MessageConverter.msg2Byte(msgBuf, bodyBuf, msg);
                bstr = MessageConverter.getWsStream(bstream, msgBuf);
                if (bstr != null) {
                    msg.setWsStream(bstr);
                }

            }
            //int ilen=bstr.length;
            if (bstr != null) {
                channel.write(bstr);
                //logger.debug(String.format(mFormat, nConnID, bstr.length));
            }

        }
        catch (Exception e) {
            logger.error("ConnectionPoint.outPut() fail:", e);
        }
    }

    public synchronized void sendMessage(IMessage msg)
    {
        if (logout) {
            return;
        }

        try {
            Message[] msgList = MessageConverter.getGroupMessage(bodyBuf, msg);
            if (msgList == null) {
                outPut(msg);
            }
            else {
                for (Message dmsg : msgList) {
                    outPut(dmsg);
                }
            }

        }
        catch (Exception e) {
            logger.error("ClientHandler.sendMessage() fail:", e);
        }
    }

    @Override
    public void sendReply(IMessage replyMsg, IMessage requstMsg)
    {
    }

    @Override
    public ClientInfo getClientInfo()
    {
        return clientInfo;
    }

    @Override
    public void logout()
    {
        logger.info("Client[" + nConnID + "] logout");
        disConnect();
        closePhysicalConnect();
    }

    @Override
    public boolean isConnected()
    {
        return !logout;
    }
}

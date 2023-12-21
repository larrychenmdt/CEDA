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

package com.adaptiveMQ.client.transport;

import com.adaptiveMQ.client.ClientInfo;
import com.adaptiveMQ.client.ClientSession;
import com.adaptiveMQ.client.ConnectionException;
import com.adaptiveMQ.client.IClientConnection;
import com.adaptiveMQ.client.IClientSession;
import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.client.IMessageListener;
import com.adaptiveMQ.client.internal.MessageProcessor;
import com.adaptiveMQ.utils.Consts;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractClientConnection implements IClientConnection
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractClientConnection.class);

    protected ClientInfo clientInfo = null;
    protected int retryTime = 3; //重试次数3
    protected long waitTimeMs = 5000; //等待时间5s
    protected String clientID = "";

    protected int eventCode = -1; //保存连接状态
    protected boolean bConnected = false; //标记是否已经建立连接

    protected int connectionID = -1;

    protected MessageProcessor messageProcessor;
    protected Vector<IEventListener> eventListeners;    //保存事件监听器列表

    protected IClientReadService reader = null;
    protected IClientWriteService writer = null;
    protected String socketAddress = null;

    protected boolean hasRun = false;    //记录是否已经启动
    protected boolean isWork = true;
    private ConcurrentHashMap<Long, IClientSession> hashSessions;        //保存创建的session

    private volatile int sessionID = 0;            //保存Session的ID值

    public AbstractClientConnection(ClientInfo info)
    {
        configureParams(info);
        eventListeners = new Vector<>();
        hashSessions = new ConcurrentHashMap<>();
        messageProcessor = new MessageProcessor(this, clientInfo); //创建消息处理器
    }

    public void configureParams(ClientInfo clientInfo)
    {
        this.clientInfo = clientInfo.clone();
    }

    public String getSocketAddress()
    {
        return socketAddress;
    }

    public void setPingInterval(int interval)
    {
        messageProcessor.setPingInterval(interval);
    }

    public void setHeartbeatInterval(int interval)
    {
        messageProcessor.setHeartbeatInterval(interval);
    }

    public String getClientID()
    {
        return clientID;
    }

    public void setClientID(String clientID)
    {
        if (clientID == null) {
            logger.error("AbstractClientConnection setClientID: clientID is null");
            return;
        }

        if (clientID.length() >= Consts.MAX_CLIENT_ID_LEN) {
            logger.error("AbstractClientConnection setClientID: clientID has to be less than " + Consts.MAX_CLIENT_ID_LEN);
            return;
        }

        this.clientID = clientID;
    }

    public int getEventCode()
    {
        return eventCode;
    }

    public boolean isConnected()
    {
        return bConnected;
    }

    public int getConnectionID()
    {
        return connectionID;
    }

    public void setConnectionID(int nConnectionID)
    {
        this.connectionID = nConnectionID;
    }

    public void addEventListener(IEventListener listener)
    {
        eventListeners.addElement(listener);
    }

    public void removeEventListener(IEventListener listener)
    {
        eventListeners.removeElement(listener);
    }

    protected int getNextSessionId()
    {
        return ++sessionID;
    }

    //保存一个新建立的session
    protected void addSession(Long key, IClientSession session)
    {
        hashSessions.put(key, session);
    }

    //得到一个新建立的session
    protected IClientSession getSession(Long key)
    {
        return hashSessions.get(key);
    }

    public void setRetryConnect(int retryTimes, long waitTime)
    {
        if (retryTimes < 0) {
            return;
        }
        if (waitTime < 0) {
            return;
        }

        retryTime = retryTimes + 1;    //需要+1,保证至少连接一次
        this.waitTimeMs = waitTime;
    }

    public void sleepTime()
    {
        try { //sendEvent(IEventListener.CONNECTION_RECONNECT);
            Thread.sleep(waitTimeMs);
        }
        catch (Exception e1) {
        }
    }

    public void sendEvent(int nCode)
    {
        if (eventCode == nCode) {
            return;
        }
        eventCode = nCode;
        //如果与服务器连接断开，则断开
        switch (nCode) {
            case IEventListener.CONNECTION_CONNECTING: {
                //m_bConnected = true;
            }
            break;
            case IEventListener.CONNECTION_CLOSED: {
                //m_vecEventListeners.clear();
                close();
            }
            break;
        }
        for (int i = 0; i < eventListeners.size(); i++) {
            IEventListener listener = eventListeners.elementAt(i);
            if (listener != null) {
                listener.onEvent(nCode);
            }
        }
    }

    //abstract public IClientSession createSession();
    public synchronized IClientSession createSession()
    {
        logger.warn("ClientConnection: Create a session");

        IClientSession session = null;
        //创建一个session
        session = new ClientSession(connectionID, getNextSessionId(), messageProcessor);
        //将session保存到列表中
        addSession(new Long(session.getSessionID()), session);

        return session;
    }

    public void setLoginListener(IMessageListener msgListener)
    {
        messageProcessor.setLoginListener(msgListener);
    }

    public void start() throws IOException, ConnectionException
    {
        isWork = true;
        if (hasRun) { //如果已经启动，不再次启动
            throw new ConnectionException("AbstractClientConnection had started, cannot start twice");
        }

        //建立与服务器连接
        connect();

        if (!isWork) {
            return;
        }

        messageProcessor.setWriter(writer);

        reader.work();
        writer.work();

        login();
    }

    public void close()
    {
        try {
            if (bConnected) {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.close();
                }
                reader = null;
                writer = null;

            }
        }
        catch (Exception e) {
            logger.error(e);
        }

        stop();

        bConnected = false;
        hasRun = false;

        sendEvent(IEventListener.CONNECTION_CLOSED);
    }

    private void login() throws ConnectionException
    {
        sendEvent(IEventListener.CONNECTION_LOGINING);

        messageProcessor.login(clientInfo, clientID);

        sendEvent(IEventListener.CONNECTION_LOGIN_SUCCESS);
    }

    protected abstract void connect() throws ConnectionException;
}

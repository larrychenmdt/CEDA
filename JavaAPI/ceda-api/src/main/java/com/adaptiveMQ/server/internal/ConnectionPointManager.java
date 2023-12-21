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

import com.adaptiveMQ.cluster.IClusterClient;
import com.adaptiveMQ.cluster.ServiceInfo;
import com.adaptiveMQ.message.BaseDestination;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.server.IServerConnection;
import com.adaptiveMQ.server.IServerConnectionMessageListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class ConnectionPointManager extends Thread
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnectionPointManager.class);
    private final ConcurrentHashMap<Integer, ConnectionPoint> connectionId2PointMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<IServerConnection, ArrayList<BaseDestination>> serverConnectionSubscriptionList = new ConcurrentHashMap<>();
    private final MessageExchanger messageExchanger;
    private final IClusterClient clustClient;
    private final ServiceInfo serviceInfo;
    private final IServerConnectionMessageListener syncMsgListenter = null;
    private int lastGeneratedConnectionID = 1000; //connection ID will be used as streamID, 0-1000 is reserved
    private long keepAliveCheckInterval = 1000 * 40;
    @Deprecated
    private int sendTimeout = 300;
    private boolean isPingThreadRunning = true;

    public ConnectionPointManager(MessageExchanger exchange, IClusterClient clusterClient, ServiceInfo sInfo)
    {
        clustClient = clusterClient;
        serviceInfo = sInfo;
        messageExchanger = exchange;
        this.start();
    }

    void close()
    {
        isPingThreadRunning = false;
        //this.interrupt();
    }

    public void disConnectClients()
    {
        Collection<ConnectionPoint> coll = connectionId2PointMap.values();
        for (ConnectionPoint client : coll) {
            client.logout();
        }
    }

    public List<IServerConnection> getClients()
    {
        List<IServerConnection> clientList = new ArrayList<IServerConnection>();
        Collection<ConnectionPoint> coll = connectionId2PointMap.values();
        for (ConnectionPoint client : coll) {
            clientList.add(client);
        }
        return clientList;
    }

    synchronized int generateNextConntionID()
    {
        lastGeneratedConnectionID++;
        return lastGeneratedConnectionID;
    }

    void onSyncMessage(Message msg, IServerConnection connHandler)
    {
        if (syncMsgListenter != null) {
            syncMsgListenter.onMessage(msg, connHandler);
        }
    }

    int getSendTimeOut()
    {
        return sendTimeout;
    }

    public void setSendTimeOut(int nTimeOut)
    {
        sendTimeout = nTimeOut;
    }

    public ArrayList<BaseDestination> getSubscribeList(IServerConnection conn)
    {
        ArrayList<BaseDestination> aret = serverConnectionSubscriptionList.get(conn);
        if (aret == null) {
            aret = new ArrayList<BaseDestination>();
            serverConnectionSubscriptionList.put(conn, aret);
        }
        return aret;
    }

    void setCheckInterval(int interval)
    {
        keepAliveCheckInterval = 1000 * interval;
    }

    public IServerConnection getConnection(int connectionID)
    {
        return connectionId2PointMap.get(new Integer(connectionID));
    }

    void loginSuccess(ConnectionPoint handler)
    {
        connectionId2PointMap.put(new Integer(handler.getConnectionID()), handler);
        handler.loginSuccess();
        if (serviceInfo != null && clustClient != null) {
            if (serviceInfo.getType() == ServiceInfo.SERVICE_TYPE_BALANCE) {
                serviceInfo.increaseClientNum();
                try {
                    clustClient.setServiceInfor(serviceInfo);
                }
                catch (Exception e) {
                    logger.error("failed to process loginSucess event", e);
                }
            }
        }
    }

    void loginFail(String userName)
    {
        logger.info(" login fail, userName=" + userName);
    }

    public void disConnect(ConnectionPoint handler)
    {
        handler.disConnect();
        connectionId2PointMap.remove(new Integer(handler.getConnectionID()));
        if (serviceInfo != null && clustClient != null) {
            if (serviceInfo.getType() == ServiceInfo.SERVICE_TYPE_BALANCE) {
                serviceInfo.decreaseClientNum();
                try {
                    clustClient.setServiceInfor(serviceInfo);
                }
                catch (Exception e) {
                    logger.warn("setServiceInfor failed", e);
                }
            }
        }
    }

    //删除订阅
    void logout(ConnectionPoint connHandler)
    {
        ArrayList<BaseDestination> subList = serverConnectionSubscriptionList.remove(connHandler);
        if (subList != null) {
            for (BaseDestination des : subList) {
                messageExchanger.unSubscribe(des, connHandler);
            }
        }
    }

    public void run()
    {
        isPingThreadRunning = true;
        while (true) {
            try {
                sleep(1000);
                if (!isPingThreadRunning) {
                    break;
                }
                long ltime = Calendar.getInstance().getTimeInMillis();
                Collection<ConnectionPoint> coll = connectionId2PointMap.values();
                for (ConnectionPoint handler : coll) {
                    if ((ltime - handler.getActiveTime()) > keepAliveCheckInterval) {
                        handler.ping();
                    }
                }
            }
            catch (Exception e) {
                logger.error("CHandlerManage.run() fail:", e);
            }
        }
    }
}

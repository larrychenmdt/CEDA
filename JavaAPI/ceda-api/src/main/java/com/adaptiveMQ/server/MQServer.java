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

package com.adaptiveMQ.server;

import com.adaptiveMQ.cluster.IClusterClient;
import com.adaptiveMQ.cluster.ServiceInfo;
import com.adaptiveMQ.message.BaseDestination;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.server.internal.ConnectionPointManager;
import com.adaptiveMQ.server.internal.MessageExchanger;
import com.adaptiveMQ.server.transport.IChannel;
import com.adaptiveMQ.server.transport.IServerTransport;
import com.adaptiveMQ.server.transport.ServerTransportFactory;
import com.adaptiveMQ.utils.Consts;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MQServer implements Runnable, IServerHandler
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MQServer.class);
    private final MessageExchanger messageExchanger = new MessageExchanger();
    private final IChannel serverChanelProxy = null;
    private IServerConnectionListener serverConnectionListener;
    private IServerLoginHandler loginHandler;
    private ConnectionPointManager connectionPointManager;
    private ServiceInfo serviceInfo;
    private IClusterClient clusterClient;

    public MQServer(ServiceInfo sInfo, IServerConnectionListener serverConnectionListener, IServerLoginHandler loginHandler, IClusterClient clustClient)
    {
        this.serviceInfo = sInfo;
        this.serverConnectionListener = serverConnectionListener;
        this.loginHandler = loginHandler;
        this.clusterClient = clustClient;

        logger.info(Consts.VERSION + ", Server start");
        logger.info("Address: " + serviceInfo.getHost());
        logger.info("Port: " + serviceInfo.getPort());

        this.connectionPointManager = new ConnectionPointManager(messageExchanger, clusterClient, serviceInfo);
    }

    public static IServerHandler startMQ2App(ServiceInfo sInfo, IServerConnectionListener serverConnectionListener, IServerLoginHandler loginHandler, IClusterClient clustClient)
    {
        MQServer mq2Server = new MQServer(sInfo, serverConnectionListener, loginHandler, clustClient);
        new Thread(mq2Server).start();
        return mq2Server;
    }

    public synchronized void addSubscription(List<BaseDestination> topicList, IServerConnection connHandler)
    {
        if (topicList == null || connHandler == null) {
            return;
        }
        ArrayList<BaseDestination> subList = connectionPointManager.getSubscribeList(connHandler);
        for (BaseDestination des : topicList) {
            if (!subList.contains(des)) {
                messageExchanger.subscribe(des, connHandler);
                subList.add(des);
            }
        }
    }

    public void removeSubscription(List<BaseDestination> topicList, IServerConnection connHandler)
    {
        if (topicList == null || connHandler == null) {
            return;
        }
        for (BaseDestination des : topicList) {
            messageExchanger.unSubscribe(des, connHandler);
        }
    }

    @Override
    public void bind(ServiceInfo serviceInfo)
    {
        IServerTransport server = ServerTransportFactory.createServer(serviceInfo.getProtocolType());
        server.initialize(connectionPointManager);
        server.setServerConnectionListener(serverConnectionListener);
        server.setServerLoginHandler(loginHandler);

        server.bind(serviceInfo);
        try {
            serviceInfo.setStat(true);
            if (clusterClient != null) {
                if (ServiceManager.getInstance().registerConnected()) {
                    clusterClient.registService(serviceInfo);
                }
                else {
                    logger.error("can't write to Zookeeper[" + serviceInfo.getName() + "] before connected");
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error("failed to bind", e);
            System.exit(0);
        }
    }

    @Override
    public void setServerConnectionEventListener(IServerConnectionListener eventListener)
    {
        this.serverConnectionListener = eventListener;
    }

    @Override
    public void setClusterClient(IClusterClient clusterClient)
    {
        this.clusterClient = clusterClient;
    }

    @Override
    public void shutdown()
    {
        serviceInfo.setStat(false);

        if (messageExchanger != null) {
            messageExchanger.close();
        }

        if (ServiceManager.getInstance().registerConnected() && (clusterClient != null)) {
            try {
                serviceInfo.setStat(false);
                if (serviceInfo.getType() == ServiceInfo.SERVICE_TYPE_STANDBY) {
                    clusterClient.setServiceInfor(serviceInfo);
                }
                else {
                    clusterClient.removeServiceInfor(serviceInfo);
                }

            }
            catch (Exception e) {
                logger.error("unable register fail, seq:" + serviceInfo.getSequenceName(), e);
            }
        }
        if (serverChanelProxy != null) {
            serverChanelProxy.close();
            logger.info("Server (port: " + serviceInfo.getPort() + ") shutdown");
        }
    }

    @Override
    public void publishMessage(Message msg)
    {
        messageExchanger.addMessage(msg);
    }

    @Override
    public IPublisher createPublisher()
    {
        return null;
    }

    @Override
    public IPublisher createCertifiedMessagePublisher(Properties props)
    {
        return null;
    }

    @Override
    public List<IServerConnection> getClients()
    {
        return connectionPointManager.getClients();
    }

    @Override
    public void disConnectClients()
    {
        connectionPointManager.disConnectClients();
    }

    @Override
    public IServerConnection getConnection(int connectionID)
    {
        return connectionPointManager.getConnection(connectionID);
    }

    @Override
    public void run()
    {
        this.bind(serviceInfo);
    }

    // Verify that this exchange instance has not been closed. This method throws IllegalStateException if the exchange
    // has already been closed.
    private void throwIfExchangeClosed()
    {
        if (messageExchanger == null || !messageExchanger.isRunning()) {
            throw new IllegalStateException("Cannot perform operation after producer has been closed");
        }
    }

    @Override
    public void close() throws IOException
    {
        //TODO
        return;
    }
}

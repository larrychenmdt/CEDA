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

package com.adaptiveMQ.example.pubsub;

import com.adaptiveMQ.client.ClientInfo;
import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.client.ProtocolType;
import com.adaptiveMQ.cluster.ServiceInfo;
import com.adaptiveMQ.example.ExampleConfiguration;
import com.adaptiveMQ.message.Destination;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.MessageBody;
import com.adaptiveMQ.server.IServerConnection;
import com.adaptiveMQ.server.IServerHandler;
import com.adaptiveMQ.server.ServiceManager;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BasicPublisherServer extends Thread
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(BasicPublisherServer.class);

    private IServerHandler serverHandler;
    private ServiceManager instance;
    private ServiceInfo svrInfo;
    final ExecutorService executor = Executors.newFixedThreadPool(1);

    public static void main(String[] args)
    {
        BasicPublisherServer adapter = new BasicPublisherServer();
        adapter.startMQApp(ExampleConfiguration.SERVER_HOST, ExampleConfiguration.SERVER_PORT);
    }

    ServiceInfo createServiceInfo(String host, int port, ProtocolType protocolType) throws Exception
    {
        ServiceInfo svrInfo = new ServiceInfo();
        svrInfo.setHost(host);
        svrInfo.setPort(port);
        svrInfo.setProtocolType(protocolType);
        svrInfo.setType(ServiceInfo.SERVICE_TYPE_STANDBY);
        svrInfo.setName("BasicPublisherServer");
        svrInfo.setShareMemoryPath("F:\\temp\\aeron_ipc\\pubsub");
        return svrInfo;
    }

    public void startMQApp(String host, int port)
    {
        logger.info("startMQ2App");

        instance = ServiceManager.getInstance();
        instance.addRegisterEventListener((int nCode) ->
        {
            switch (nCode) {
                case IEventListener.CONNECTION_CONNECTING:
                    logger.info("Event: CONNECTION_CONNECTING");
                    break;
                case IEventListener.CONNECTION_CONNECTED:
                    logger.info("Event: CONNECTION_CONNECTED");
                    break;
                case IEventListener.CONNECTION_CLOSED:
                    logger.info("Event: CONNECTION_CLOSED");
                    break;
                case IEventListener.CONNECTION_RECONNECT:
                    logger.info("Event: CONNECTION_RECONNECTED");
                    break;
                case IEventListener.CONNECTION_LOGINING:
                    logger.info("Event: CONNECTION_LOGINING");
                    break;
                case IEventListener.CONNECTION_LOGIN_SUCCESS:
                    logger.info("Event: CONNECTION_LOGIN_SUCCESS");
                    break;
                case IEventListener.CONNECTION_IO_EXCEPTION:
                    logger.info("Event: CONNECTION_IO_EXCEPTION");
                    break;
                case IEventListener.CONNECTION_LOST:
                    logger.info("Event: CONNECTION_LOST");
                    break;
                case IEventListener.CONNECTION_TIMEOUT:
                    logger.info("Event: CONNECTION_TIMEOUT");
                    break;
            }
        });

        try {
            svrInfo = createServiceInfo(host, port, ExampleConfiguration.protocolType);
            serverHandler = instance.startServer(svrInfo,
                    (int nCode, IServerConnection connHandler) ->
                    {
                        switch (nCode) {
                            case IEventListener.CONNECTION_CLOSED:
                                logger.info("Event: CONNECTION_CLOSED, user=" + connHandler.getClientInfo().getUsername());
                                break;
                            case IEventListener.CONNECTION_LOGIN_SUCCESS:
                                logger.info("Event: CONNECTION_LOGIN_SUCCESS, user=" + connHandler.getClientInfo().getUsername());
                                break;
                            default:
                                logger.info("Event: unknow," + nCode);
                        }
                    },
                    (ClientInfo cInfo, IServerConnection connHandler)-> {
                        connHandler.setMessageHandler((msg, connection) -> {
                            try {
                                logger.info("on Message, topic:" + ((Message)msg).getDestination().getName()
                                        + ", data:" + msg.getMessageBody().toString());
                            }
                            catch (Exception e) {
                                logger.error(e);
                            }});

                        connHandler.setSubscriptionHandler((topicList, connection, isSubcribe) -> {
                            if(isSubcribe) {
                                serverHandler.addSubscription(topicList, connHandler);
                                executor.submit(()-> {
                                    for(int count = 0; count < 1000; count++) {
                                        Message msg = new Message();
                                        msg.setDestination(new Destination("TEST.PUBLISH"));
                                        MessageBody body = msg.getMessageBody();
                                        try {
                                            body.addInt((short) 5, 0);
                                            body.addInt((short) 7, 123456789);
                                            body.addLong((short) 9, 123456789123456789l);
                                            body.addShort((short) 11, (short) 1234);
                                            body.addString((short) 3, "update_count_" + count);
                                            serverHandler.publishMessage(msg);
                                        } catch (Exception exception) {
                                            logger.error(exception);
                                        }
                                        try {
                                            Thread.sleep(1);
                                        } catch (InterruptedException interruptedException) {
                                            interruptedException.printStackTrace();
                                        }
                                    }
                                });
                            }
                            else {
                                serverHandler.removeSubscription(topicList, connHandler);
                            }
                        });
                        return true;
                    });
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
        }
    }
}

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

package com.adaptiveMQ.example.rpc;

import com.adaptiveMQ.client.ClientInfo;
import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.cluster.ServiceInfo;
import com.adaptiveMQ.example.ExampleConfiguration;
import com.adaptiveMQ.message.IMessage;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.server.IServerHandler;
import com.adaptiveMQ.server.IServerConnection;
import com.adaptiveMQ.server.ServiceManager;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class PongServer extends Thread
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PongServer.class);

    private IServerHandler serverHandler;
    private ServiceManager instance;

    public static void main(String[] args) throws Exception
    {
        PongServer pongServer = new PongServer();
        ServiceInfo svrInfo = createServiceInfo();
        pongServer.startApp(svrInfo);
    }

    public static ServiceInfo createServiceInfo() throws Exception
    {
        ServiceInfo svrInfo = new ServiceInfo();
        svrInfo.setHost(ExampleConfiguration.SERVER_HOST);
        svrInfo.setPort(ExampleConfiguration.SERVER_PORT);
        svrInfo.setProtocolType(ExampleConfiguration.protocolType);
        svrInfo.setType(ServiceInfo.SERVICE_TYPE_STANDBY);
        svrInfo.setName(ExampleConfiguration.PONG_SERVER_NAME);
        svrInfo.setShareMemoryPath(ExampleConfiguration.SHM_BASE_PATH + ExampleConfiguration.PONG_SERVER_NAME);
        return svrInfo;
    }

    public void startApp(ServiceInfo svrInfo)
    {
        logger.info("startApp");
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
            serverHandler = instance.startServer(svrInfo,
                    (int nCode, IServerConnection connHandler)-> {
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
                    (ClientInfo cInfo, IServerConnection connHandler) -> {
                        connHandler.setMessageHandler((imsg, connection) -> {
                            if (imsg.getMessageType() == IMessage.MESSAGE_TYPE_DATA) {
                                Message msg = (Message) imsg;
                                try {
                                    long requestTimeStamp = msg.getMessageBody().getLong((short) 6);
                                    long diff = System.currentTimeMillis() - requestTimeStamp;
                                    logger.info("on Message, topic:" + msg.getDestination().getName()
                                            + ", data:" + msg.getMessageBody().getString((short) 5) + ", elapsed:" + diff + "(ms)");
                                    Message rmsg = msg.createReplyMessage();
                                    rmsg.getMessageBody().addString((short) 3, "pong");
                                    connHandler.sendMessage(rmsg);
                                }
                                catch (Exception e) {
                                    logger.error(e);
                                }
                            }
                        });
                        connHandler.setSubscriptionHandler((topicList, connection, isSubcribe) -> {
                            if(isSubcribe) {
                                serverHandler.addSubscription(topicList, connHandler);
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

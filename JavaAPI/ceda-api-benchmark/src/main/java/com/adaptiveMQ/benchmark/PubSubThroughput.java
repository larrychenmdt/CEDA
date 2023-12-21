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

package com.adaptiveMQ.benchmark;

import com.adaptiveMQ.client.ClientConnectionFactory;
import com.adaptiveMQ.client.ClientInfo;
import com.adaptiveMQ.client.IClientConnection;
import com.adaptiveMQ.client.IClientSession;
import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.client.ProtocolType;
import com.adaptiveMQ.cluster.ServiceInfo;
import com.adaptiveMQ.message.Destination;
import com.adaptiveMQ.message.IMessage;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.MessageBody;
import com.adaptiveMQ.message.internal.MessageConverter;
import com.adaptiveMQ.server.IServerHandler;
import com.adaptiveMQ.server.IServerConnection;
import com.adaptiveMQ.server.ServiceManager;
import com.adaptiveMQ.utils.BufferByte;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PubSubThroughput
{
    public static final String NUMBER_OF_MESSAGES_PROP = "ceda.pubsub.sample.messages";

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PubSubThroughput.class);
    private static final long NUMBER_OF_MESSAGES = Long.getLong(NUMBER_OF_MESSAGES_PROP, 10_000_000);;
    static final RateReporter subscriberReporter = new RateReporter(
            TimeUnit.SECONDS.toNanos(1), PubSubThroughput::printSubscriberRate);
    static final RateReporter publisherReporter = new RateReporter(
            TimeUnit.SECONDS.toNanos(1), PubSubThroughput::printPublisherRate);

    static final ExecutorService executor = Executors.newFixedThreadPool(3);
    static IServerHandler serverHandler = null;
    static final long messageSize = calcMessageSize();
    static final long PUBSUB_MAX_GAP_MESSAGE_COUNT = 10000;

    public static void main(String[] args) throws Exception
    {
        logger.info("start PubSub Throughput Test");
        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) ->
                {
                    logger.error("Uncaught exception on: " + t, e);
                    e.printStackTrace(System.out);
                });

        ServiceManager instance = ServiceManager.getInstance();
        instance.addRegisterEventListener(
            (event) -> {
                printEvent(event);
            });
        ServiceInfo svrInfo = createServiceInfo(BenchmarkConfiguration.SERVER_HOST,
            BenchmarkConfiguration.SERVER_PORT, BenchmarkConfiguration.SHM_IPC_PATH, BenchmarkConfiguration.protocolType);
            serverHandler = instance.startServer(svrInfo, (code, connection)-> {
                switch (code) {
                case IEventListener.CONNECTION_CLOSED:
                    logger.info("Event: CONNECTION_CLOSED, user=" + connection.getClientInfo().getUsername());
                    break;
                case IEventListener.CONNECTION_LOGIN_SUCCESS:
                    logger.info("Event: CONNECTION_LOGIN_SUCCESS, user=" + connection.getClientInfo().getUsername());
                    break;
                default:
                    logger.info("Event: unknow," + code);
            }},
            (ClientInfo cInfo, IServerConnection connHandler) -> {
                connHandler.setMessageHandler((imsg, connection) -> {
                    Message msg = (Message) imsg;
                    try {
                        logger.info("on Message, topic:" + msg.getDestination().getName()
                                + ", data:" + msg.getMessageBody().toString());
                    }
                    catch (Exception e) {
                        logger.error(e);
                    }
                });
                connHandler.setSubscriptionHandler((topicList, connection, isSubscribe) -> {
                    if(isSubscribe) {
                        serverHandler.addSubscription(topicList, connHandler);
                        executor.submit(()-> {
                            for(int count = 0; count < NUMBER_OF_MESSAGES; count++) {
                                try {
                                    serverHandler.publishMessage(createMessage(0));
                                    publisherReporter.onMessage(messageSize);
                                    if (publisherReporter.totalMessages - subscriberReporter.totalMessages > PUBSUB_MAX_GAP_MESSAGE_COUNT) {
                                        try {
                                            Thread.sleep(10);
                                        }
                                        catch (InterruptedException e) {}
                                    }
                                } catch (Exception exception) {
                                    logger.error(exception);
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
        Thread.sleep(10000);

        ClientInfo clientInfo = createClientInfo(BenchmarkConfiguration.SERVER_HOST,
                BenchmarkConfiguration.SERVER_PORT,
                BenchmarkConfiguration.LOGIN_USER_NAME, BenchmarkConfiguration.LOGIN_PASSWORD, BenchmarkConfiguration.SHM_IPC_PATH,
                BenchmarkConfiguration.protocolType, BenchmarkConfiguration.CLIENT_HOST, BenchmarkConfiguration.CLIENT_PORT);
        IClientConnection conn = ClientConnectionFactory.createConnection(clientInfo);
        conn.addEventListener((event) -> {
            printEvent(event);
        });
        conn.start();
        IClientSession session = conn.createSession();

        executor.execute(subscriberReporter);
        executor.execute(publisherReporter);
        session.subscribe(Arrays.asList(new Destination("TEST.PUBLISH")), msg -> {
            subscriberReporter.onMessage(messageSize);
        });
    }

    static ServiceInfo createServiceInfo(String host, int port, String shmPath, ProtocolType protocolType) throws Exception
    {
        ServiceInfo svrInfo = new ServiceInfo();
        svrInfo.setHost(host);
        svrInfo.setPort(port);
        svrInfo.setProtocolType(protocolType);
        svrInfo.setType(ServiceInfo.SERVICE_TYPE_STANDBY);
        svrInfo.setName("BasicPublisherServer");
        svrInfo.setShareMemoryPath(shmPath);
        return svrInfo;
    }

    private static ClientInfo createClientInfo(String serverHost, int serverPort, String user, String password, String shmPath, ProtocolType protocolType, String clientHost, int clientPort)
    {
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setUser(user, password);
        clientInfo.setAddress(serverHost, serverPort);
        clientInfo.setProtocol(protocolType);
        clientInfo.setLocalHost(clientHost);
        clientInfo.setLocalPort(clientPort);
        clientInfo.setShareMemoryPath(shmPath);
        return clientInfo;
    }

    private static long calcMessageSize()
    {
        try {
            IMessage msg = createMessage(0);
            BufferByte msgBuf = new BufferByte();
            BufferByte bodyBuf = new BufferByte();
            byte[] bstream = MessageConverter.msg2Byte(msgBuf, bodyBuf, msg);
            byte[] bstr = MessageConverter.getWsStream(bstream, msgBuf);
            return bstr.length;
        }
        catch (Exception e){
            logger.error(e);
        }
        return 0;
    }

    private static Message createMessage(int index)
    {
        Message msg = new Message();
        msg.setDestination(new Destination("TEST.PUBLISH"));
        MessageBody body = msg.getMessageBody();
        try {
            body.addInt((short) 5, 0);
            body.addInt((short) 7, 123456789);
            body.addLong((short) 9, 123456789123456789l);
            body.addShort((short) 11, (short) 1234);
            body.addString((short) 3, "update_count_" + index);
        } catch (Exception exception) {
            logger.error(exception);
        }
        return msg;
    }

    private static void printSubscriberRate(
            double messagesPerSec, double bytesPerSec, long totalMessages, long totalBytes)
    {
        logger.info(String.format(
                    "receive: %.04g msgs/sec, %.04g bytes/sec, totals %d messages %d MB payloads%n",
                    messagesPerSec, bytesPerSec, totalMessages, totalBytes / (1024 * 1024)));
    }

    private static void printPublisherRate(
            double messagesPerSec, double bytesPerSec, long totalMessages, long totalBytes)
    {
        logger.info(String.format(
                "publish: %.04g msgs/sec, %.04g bytes/sec, totals %d messages %d MB payloads%n",
                messagesPerSec, bytesPerSec, totalMessages, totalBytes / (1024 * 1024)));
    }

    public static void printEvent(int nCode)
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
    }
}

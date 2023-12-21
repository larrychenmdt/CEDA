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

import com.adaptiveMQ.client.ClientConnectionFactory;
import com.adaptiveMQ.client.ClientInfo;
import com.adaptiveMQ.client.ConnectionException;
import com.adaptiveMQ.client.IClientConnection;
import com.adaptiveMQ.client.IClientSession;
import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.client.IMessageListener;
import com.adaptiveMQ.client.ProtocolType;
import com.adaptiveMQ.example.ExampleConfiguration;
import com.adaptiveMQ.message.Destination;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public class BasicSubscribeClient
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(BasicSubscribeClient.class);

    private String serverHost = ExampleConfiguration.SERVER_HOST;
    private int serverPort = ExampleConfiguration.SERVER_PORT;
    private String clientHost = ExampleConfiguration.CLIENT_HOST;
    private int clientPort = ExampleConfiguration.CLIENT_PORT;

    public static void main(String[] args) throws Exception
    {
        BasicSubscribeClient basicSubscribeClient = new BasicSubscribeClient();
        basicSubscribeClient.start();
    }

    public void start() throws Exception
    {
        IClientConnection conn = connect();
        IClientSession session = conn.createSession();
        IMessageListener listener = (msg) ->  {
            //Iterator<MessageRecord.CField> iterator = msg.getMessageBody().getFieldIterator();
           logger.info("receive message:");
            //while (iterator.hasNext()) {
            //    MessageRecord.CField cField = iterator.next();
                //logger.info("    " + cField.nPosition + ": " + cField.oField);
            //}
        };
        session.subscribe(Arrays.asList(new Destination("TEST.PUBLISH")), listener);
    }


    private IClientConnection connect() throws IOException, ConnectionException
    {
        ClientInfo info = createClientInfo(serverHost, serverPort,
                ExampleConfiguration.LOGIN_USER_NAME, ExampleConfiguration.LOGIN_PASSWORD,
                ExampleConfiguration.protocolType, clientHost, clientPort);
        IClientConnection conn = ClientConnectionFactory.createConnection(info);
        conn.addEventListener((int nCode) ->
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
        conn.start();
        return conn;
    }

    private ClientInfo createClientInfo(String serverHost, int serverPort, String user, String password,
                                ProtocolType protocolType, String clientHost, int clientPort)
    {
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setUser(user, password);
        clientInfo.setAddress(serverHost, serverPort);
        clientInfo.setProtocol(protocolType);
        clientInfo.setLocalHost(clientHost);
        clientInfo.setLocalPort(clientPort);
        clientInfo.setShareMemoryPath("F:\\temp\\aeron_ipc\\pubsub");
        return clientInfo;
    }
}

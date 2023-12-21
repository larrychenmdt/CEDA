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

import com.adaptiveMQ.client.ClientConnectionFactory;
import com.adaptiveMQ.client.ClientInfo;
import com.adaptiveMQ.client.ConnectionException;
import com.adaptiveMQ.client.IClientConnection;
import com.adaptiveMQ.client.IClientSession;
import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.example.ExampleConfiguration;
import com.adaptiveMQ.message.Destination;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.MessageBody;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;

public class PingClient
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PingClient.class);

    public static void main(String[] args) throws Exception
    {
        PingClient requestClient = new PingClient();
        ClientInfo clientInfo = createClientInfo();
        requestClient.start(clientInfo);
    }

    public static ClientInfo createClientInfo() throws Exception
    {
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setUser(ExampleConfiguration.LOGIN_USER_NAME, ExampleConfiguration.LOGIN_PASSWORD);
        clientInfo.setAddress(ExampleConfiguration.SERVER_HOST, ExampleConfiguration.SERVER_PORT);
        clientInfo.setProtocol(ExampleConfiguration.protocolType);
        clientInfo.setLocalHost(ExampleConfiguration.CLIENT_HOST);
        clientInfo.setLocalPort(ExampleConfiguration.CLIENT_PORT);
        clientInfo.setShareMemoryPath(ExampleConfiguration.SHM_BASE_PATH + ExampleConfiguration.PONG_SERVER_NAME);
        return clientInfo;
    }

    private IClientConnection createConnection(ClientInfo clientInfo) throws IOException, ConnectionException
    {
        IClientConnection conn = ClientConnectionFactory.createConnection(clientInfo);
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

    public void start(ClientInfo clientInfo) throws Exception
    {
        IClientConnection conn = createConnection(clientInfo);
        IClientSession session = conn.createSession();

        for (int count = 0; count < 100; ++count) {
            Message msgRequest = new Message();
            msgRequest.setDestination(new Destination("CEDA.RPC.PING"));
            MessageBody record = msgRequest.getMessageBody();
            record.addString((short) 5, "ping");
            long requestTimeStamp = System.currentTimeMillis();
            record.addLong((short) 6, requestTimeStamp);
            Message reply = session.sendRequest(msgRequest, 5000);
            if (reply != null) {
                long diff = System.currentTimeMillis() - requestTimeStamp;
                logger.warn(String.format("Receive reply : id=%d, topic=%s, result=%s, elapsed=%d(ms)",
                        reply.getMessageID(), reply.getDestination().getName(), reply.getMessageBody().getString((short) 3), diff));
            }
            else {
                logger.info("Timeout, can't receieve the reply");
            }
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
    }
}

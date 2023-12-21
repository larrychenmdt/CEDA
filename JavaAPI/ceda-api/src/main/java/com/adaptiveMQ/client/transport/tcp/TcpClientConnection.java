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

package com.adaptiveMQ.client.transport.tcp;

import com.adaptiveMQ.client.ClientInfo;
import com.adaptiveMQ.client.ConnectionException;
import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.client.transport.AbstractClientConnection;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public final class TcpClientConnection extends AbstractClientConnection
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TcpClientConnection.class);

    private Socket socket = null;
    private OutputStreamWriteService writeS = null;
    private InputStreamReadService readS = null;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;

    public TcpClientConnection(ClientInfo info)
    {
        super(info);
    }

    public void connect() throws ConnectionException
    {
        sendEvent(IEventListener.CONNECTION_CONNECTING);
        try {
            socket = new Socket(clientInfo.getAddressHost(), clientInfo.getAddressPort());

            socketAddress = socket.getLocalSocketAddress().toString().substring(1);
            inputStream = socket.getInputStream();
            readS = new InputStreamReadService(messageProcessor);
            readS.setStream(inputStream);
            reader = readS;

            outputStream = socket.getOutputStream();
            writeS = new OutputStreamWriteService(messageProcessor);
            writer = writeS;
            writeS.setStream(outputStream);

            //m_oSocket.setTcpNoDelay(true);
            bConnected = true;
            hasRun = true;
        }
        catch (IOException e) {
            logger.error("failed to connect", e);
        }

        if (bConnected && !hasRun) { //连接上
            //sendEvent(IEventListener.CONNECTION_CLOSED);
            close();
            throw new ConnectionException("Connection refused");
        }
    }

    public void stop()
    {
        try {
            if (socket != null) {
                socket.close();
            }
        }
        catch (Exception e) {
            logger.warn("failed to stop", e);
        }
    }
}

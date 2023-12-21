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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public final class SslClientConnection extends AbstractClientConnection
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SslClientConnection.class);

    private SSLContext sslcontext = null;
    private Socket oSocket = null;
    private OutputStreamWriteService writeS = null;
    private InputStreamReadService readS = null;
    private InputStream oInput = null;
    private OutputStream oOutput = null;

    public SslClientConnection(ClientInfo info)
    {
        super(info);
        //setProxy(info);
    }

    private void initSSLContext() throws Exception
    {
        sslcontext = SSLContext.getInstance("SSL");

        //sslcontext.init(null, new TrustManager[]{new TrustAnyTrustManager()}, new java.security.SecureRandom());
        TrustManager[] tm = {new TrustAnyTrustManager()};
        sslcontext.init(null, tm, null);
    }

    public void connect() throws ConnectionException
    {
        sendEvent(IEventListener.CONNECTION_CONNECTING);

        try {
            initSSLContext();
        }
        catch (Exception e) {
            throw new ConnectionException(e.toString());
        }

        try {
            oSocket = sslcontext.getSocketFactory().createSocket(clientInfo.getAddressHost(), clientInfo.getAddressPort());

            bConnected = true;
            // m_oReader= m_oSocket.getOutputStream();
            socketAddress = oSocket.getLocalSocketAddress().toString().substring(1);

            // m_oReader = new
            // AbstractReadService(m_oMsgProcessor,m_oInput);
            oInput = oSocket.getInputStream();
            readS = new InputStreamReadService(messageProcessor);
            readS.setStream(oInput);
            reader = readS;

            oOutput = oSocket.getOutputStream();
            writeS = new OutputStreamWriteService(messageProcessor);
            writer = writeS;
            writeS.setStream(oOutput);
            hasRun = true;
            //break;
        }
        catch (IOException e) {
            logger.error("failed to connect", e);
        }
        if (bConnected && !hasRun) { // 连接上
            // sendEvent(IEventListener.CONNECTION_CLOSED);
            close();
            throw new ConnectionException("Connection refused");
        }
    }

    public void stop() {}

    //自定义私有类
    private class TrustAnyTrustManager implements javax.net.ssl.TrustManager, X509TrustManager
    {
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
            //System.out.println("检查客户端的可信任状态...");
            return;
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
            //System.out.println("检查服务器的可信任状态");
            return;
        }

        public X509Certificate[] getAcceptedIssuers()
        {
            //return new X509Certificate[]{};
            //System.out.println("获取接受的发行商数组...");
            return null;
        }
    }
}

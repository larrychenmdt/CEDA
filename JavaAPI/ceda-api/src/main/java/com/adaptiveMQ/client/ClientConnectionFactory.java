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

package com.adaptiveMQ.client;

import com.adaptiveMQ.client.transport.aeron.AeronClientConnection;
import com.adaptiveMQ.client.transport.tcp.SslClientConnection;
import com.adaptiveMQ.client.transport.tcp.TcpClientConnection;
import com.adaptiveMQ.client.transport.ws.WsClientConnection;
import com.adaptiveMQ.client.transport.ws.WssClientConnection;
import com.adaptiveMQ.utils.Consts;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class ClientConnectionFactory
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClientConnectionFactory.class);

    /**
     * Utility classes should not have a public constructor
     */
    private ClientConnectionFactory() {}

    /**
     * 返回API版本信息
     *
     * @param null
     * @return String：API版本信息
     * @throws
     */
    public static String getVersion()
    {
        return Consts.VERSION;
    }

    /**
     * 创建和MQ连接
     *
     * @param ClientInfo info ： 客户端登录信息
     * @return IClientConnection: 和MQ连接
     * @throws ConnectionException
     */
    public static IClientConnection createConnection(ClientInfo info) throws ConnectionException
    {
        logger.info(getVersion());
        logger.info("Address: " + info.getAddressHost());
        logger.info("Port: " + info.getAddressPort() + ", Protocol: " + ClientInfo.getProtocolName(info.getProtocol()));

        if (info.getProtocol() == ProtocolType.PROTOCOL_TCP) {
            return new TcpClientConnection(info);
        }
        else if (info.getProtocol() == ProtocolType.PROTOCOL_TCPS) {
            return new SslClientConnection(info);
        }
        else if (info.getProtocol() == ProtocolType.PROTOCOL_HTTP) {
            return new WsClientConnection(info);
        }
        else if (info.getProtocol() == ProtocolType.PROTOCOL_HTTPS) {
            return new WssClientConnection(info);
        }
        else if (info.getProtocol() == ProtocolType.PROTOCOL_AERON_UDP || info.getProtocol() == ProtocolType.PROTOCOL_AERON_IPC) {
            return new AeronClientConnection(info);
        }
        else {
            throw new ConnectionException("Unsupported protocol: " + info.getProtocol());
        }
    }
}

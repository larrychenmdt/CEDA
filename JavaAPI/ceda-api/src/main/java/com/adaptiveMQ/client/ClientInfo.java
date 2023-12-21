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

import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.utils.Consts;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.agrona.SystemUtil;

import static com.adaptiveMQ.utils.Consts.AERON_SERVER_DEFAULT_ACCEPT_STREAM_ID;

public class ClientInfo
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClientInfo.class);
    private String hostName = "localhost";
    private int port = 8088;
    private String userName;
    private String password;
    private ProtocolType protocol = ProtocolType.PROTOCOL_TCP;
    private int logginConnID = -1;
    private Message loginMsg = null;
    private String sid = "";

    private String requestChannel;
    // private int requestStreamID = AERON_SERVER_DEFAULT_ACCEPT_STREAM_ID;
    private int requestStreamID = AERON_SERVER_DEFAULT_ACCEPT_STREAM_ID;

    private int responseStreamID = (int)SystemUtil.getPid();
    private String responseChannel;

    private String shareMemoryPath;

    public String getLocalHost()
    {
        return localHost;
    }

    public void setLocalHost(String localHost)
    {
        this.localHost = localHost;
    }

    public int getLocalPort()
    {
        return localPort;
    }

    public void setLocalPort(int localPort)
    {
        this.localPort = localPort;
    }

    private String localHost = "localhost";
    private int localPort = 20123;

    /**
     * 获取连接方式名称
     *
     * @param protocol 连接方式代码
     * @return String：连接方式名称
     * @throws
     */
    public static String getProtocolName(ProtocolType protocol)
    {
        return protocol.name();
    }

    /**
     * 设置连接地址，端口
     *
     * @param String host：地址, int port：端口
     * @return void
     * @throws
     */
    public void setAddress(String host, int port)
    {
        String[] strlist = host.split("/");
        hostName = strlist[0];
        this.port = port;
    }

    /**
     * 设置连接用户名，密码
     *
     * @param String username：用户名, String password：密码
     * @return void
     * @throws
     */
    public void setUser(String username, String password)
    {
        if (username == null || password == null) {
            logger.error("ClientInfo setUser: username or password is null");
            return;
        }

        if (username.length() >= Consts.MAX_USERNAME_LEN) {
            logger.error("ClientInfo setUser: username has to be less than " + Consts.MAX_USERNAME_LEN);
            return;
        }

        if (password.length() >= Consts.MAX_PASSWORD_LEN) {
            logger.error("ClientInfo setUser: password has to be less than " + Consts.MAX_PASSWORD_LEN);
            return;
        }

        userName = username;
        this.password = password;
    }

    /**
     * 获取连接地址
     *
     * @param null
     * @return String：连接地址
     * @throws
     */
    public String getAddressHost()
    {
        return hostName;
    }

    /**
     * 获取连接端口
     *
     * @param null
     * @return int：连接端口
     * @throws
     */
    public int getAddressPort()
    {
        return port;
    }

    /**
     * 获取登录用户名
     *
     * @param null
     * @return String：登录用户名
     * @throws
     */
    public String getUsername()
    {
        return userName;
    }

    /**
     * 获取登录密码
     *
     * @param null
     * @return String：登录密码
     * @throws
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * 获取连接方式
     *
     * @param null
     * @return ProtocolType：连接方式
     * @throws
     */
    public ProtocolType getProtocol()
    {
        return protocol;
    }

    /**
     * 设置连接方式
     *
     * @param protocol：连接方式（有TCP,HTTP,HTTPS），默认为TCP方式
     * @return void
     * @throws
     */
    public void setProtocol(ProtocolType protocol)
    {
        this.protocol = protocol;
    }

    /**
     * 获取连接ID
     *
     * @param null
     * @return int：连接ID
     * @throws
     */
    public int getLoginConnID()
    {
        return logginConnID;
    }

    /**
     * 设置连接ID号
     *
     * @param int nLoginConnID：连接ID号
     * @return void
     * @throws
     */
    public void setLoginConnID(int nLoginConnID)
    {
        logginConnID = nLoginConnID;
    }

    public String getSID()
    {
        return sid;
    }

    public void setSID(String sid)
    {
        this.sid = sid;
    }

    public Message getLoginMessage()
    {
        return loginMsg;
    }

    public void setLoginMessage(Message msg)
    {
        loginMsg = msg;
    }

    public ClientInfo clone()
    {
        ClientInfo nClient = new ClientInfo();
        nClient.setAddress(this.getAddressHost(), this.getAddressPort());
        nClient.setUser(this.getUsername(), this.getPassword());
        nClient.setLoginMessage(this.getLoginMessage());
        nClient.setProtocol(this.getProtocol());
        nClient.setLoginConnID(this.getLoginConnID());
        nClient.setSID(this.getSID());
        nClient.setShareMemoryPath(this.getShareMemoryPath());
        return nClient;
    }

    public int getRequestStreamID()
    {
        return requestStreamID;
    }

    public void setRequestStreamID(int requestStreamID)
    {
        this.requestStreamID = requestStreamID;
    }

    public String getRequestChannel()
    {
        return requestChannel;
    }

    public void setRequestChannel(String requestChannel)
    {
        this.requestChannel = requestChannel;
    }

    public int getResponseStreamID()
    {
        return responseStreamID;
    }

    public void setResponseStreamID(int responseStreamID)
    {
        this.responseStreamID = responseStreamID;
    }

    public String getResponseChannel()
    {
        return responseChannel;
    }

    public void setResponseChannel(String responseChannel)
    {
        this.responseChannel = responseChannel;
    }

    public String getShareMemoryPath()
    {
        return shareMemoryPath;
    }

    public void setShareMemoryPath(String shareMemoryPath)
    {
        this.shareMemoryPath = shareMemoryPath;
    }

    @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer("ClientInfo{");
        sb.append("hostName='").append(hostName).append('\'');
        sb.append(", port=").append(port);
        sb.append(", userName='").append(userName).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append(", protocol=").append(protocol);
        sb.append(", logginConnID=").append(logginConnID);
        sb.append(", loginMsg=").append(loginMsg);
        sb.append(", sid='").append(sid).append('\'');
        sb.append(", requestChannel='").append(requestChannel).append('\'');
        sb.append(", requestStreamID=").append(requestStreamID);
        sb.append(", responseStreamID=").append(responseStreamID);
        sb.append(", responseChannel='").append(responseChannel).append('\'');
        sb.append(", shareMemoryPath='").append(shareMemoryPath).append('\'');
        sb.append(", localHost='").append(localHost).append('\'');
        sb.append(", localPort=").append(localPort);
        sb.append('}');
        return sb.toString();
    }
}

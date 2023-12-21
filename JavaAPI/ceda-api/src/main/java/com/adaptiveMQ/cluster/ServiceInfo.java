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

package com.adaptiveMQ.cluster;

import com.adaptiveMQ.client.ProtocolType;
import com.adaptiveMQ.server.ServiceType;

import static com.adaptiveMQ.utils.Consts.AERON_SERVER_DEFAULT_ACCEPT_STREAM_ID;

public class ServiceInfo
{
    private ProtocolType protocolType = ProtocolType.PROTOCOL_TCP;

    public static final byte SERVICE_TYPE_ALL = 1;    //

    public static final byte SERVICE_TYPE_STANDBY = 2;    //

    public static final byte SERVICE_TYPE_BALANCE = 3;

    public static final byte SERVICE_TYPE_ROUNDROBIN = 4;

    //static final public byte SERVICE_TYPE_WIGHTED_ROUND_ROBIN = 3;

    public static final String[] SERVICE_TYPE = {"UNKNOWN", "ALL", "STANDBY", "BALANCE", "ROUNDROBIN"};

    private static boolean bTcpNoDely = false;
    public boolean bActive = false;
    private String sName;
    private String sHost;
    private String seqName;
    private int nPort;
    private int nLBFactor;
    private byte bType;
    private ServiceType serviceType = ServiceType.SERVICE_TYPE_STANDBY;
    private boolean bRun;
    private int nOnlineCount; //m_nComputCount,

    private String shareMemoryPath;

    public int getAeronAcceptStreamID()
    {
        return aeronAcceptStreamID;
    }

    public void setAeronAcceptStreamID(int aeronAcceptStreamID)
    {
        this.aeronAcceptStreamID = aeronAcceptStreamID;
    }

    private int aeronAcceptStreamID = AERON_SERVER_DEFAULT_ACCEPT_STREAM_ID;

    public ServiceInfo()
    {
        nLBFactor = 1;
        //m_nComputCount=0;
        nOnlineCount = 0;
        bActive = false;
        sHost = "";
        sName = null;
        nPort = 0;
        bRun = false;
        seqName = null;
    }

    public ProtocolType getProtocolType()
    {
        return protocolType;
    }

    public void setProtocolType(ProtocolType protocolType)
    {
        this.protocolType = protocolType;
    }

    public static boolean getTcpNoDelay()
    {
        return bTcpNoDely;
    }

    public static void setTcpNoDelay(boolean tcpNODelay)
    {
        bTcpNoDely = tcpNODelay;
    }

    public boolean getStat()
    {
        return bRun;
    }

    public void setStat(boolean bRun)
    {
        this.bRun = bRun;
    }

    public String getName()
    {
        return sName;
    }

    public void setName(String name)
    {
        sName = name;
    }

    public String getSequenceName()
    {
        return seqName;
    }

    public void setSequenceName(String seqName)
    {
        this.seqName = seqName;
    }

    public int getPort()
    {
        return nPort;
    }

    public void setPort(int nport)
    {
        nPort = nport;
    }

    public byte getType()
    {
        return bType;
    }

    public void setType(byte bType)
    {
        this.bType = bType;
    }

    public String getHost()
    {
        return sHost;
    }

    public void setHost(String sHost) throws Exception
    {
        if (sHost == null || sHost.trim().length() < 1) {
            throw new Exception("host can't null");

        }
        else {
            this.sHost = sHost;
        }
    }

    public int getLBFactor()
    {
        return nLBFactor;
    }

    public void setLBFactor(int ncount)
    {
        nLBFactor = ncount;
    }

    public int getClientNum()
    {
        return nOnlineCount;
    }

    public void setClientNum(int ncount)
    {
        nOnlineCount = ncount;
    }

    public void increaseClientNum()
    {
        nOnlineCount++;
    }

    public void decreaseClientNum()
    {
        if (nOnlineCount > 0) {
            nOnlineCount--;
        }
    }

    public ServiceType getServiceType()
    {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType)
    {
        this.serviceType = serviceType;
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
        final StringBuffer sb = new StringBuffer("ServiceInfo{");
        sb.append("protocolType=").append(protocolType);
        sb.append(", bActive=").append(bActive);
        sb.append(", sName='").append(sName).append('\'');
        sb.append(", sHost='").append(sHost).append('\'');
        sb.append(", seqName='").append(seqName).append('\'');
        sb.append(", nPort=").append(nPort);
        sb.append(", nLBFactor=").append(nLBFactor);
        sb.append(", bType=").append(bType);
        sb.append(", serviceType=").append(serviceType);
        sb.append(", bRun=").append(bRun);
        sb.append(", nOnlineCount=").append(nOnlineCount);
        sb.append(", shareMemoryPath='").append(shareMemoryPath).append('\'');
        sb.append(", aeronAcceptStreamID=").append(aeronAcceptStreamID);
        sb.append('}');
        return sb.toString();
    }
}

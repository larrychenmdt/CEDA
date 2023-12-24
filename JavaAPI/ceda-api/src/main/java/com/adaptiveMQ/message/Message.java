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

package com.adaptiveMQ.message;

import com.adaptiveMQ.utils.Consts;
import com.adaptiveMQ.utils.ConstsMessage;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class Message extends IMessage
{
    /**
     * Cache消息类型
     */
    public static final byte CACHE = 2;
    /**
     * MQ2消息类型
     */
    public static final byte DATA_MQ = 0;
    /**
     * 点对点消息类型
     */
    public static final byte DATA_P2P = 1;
    //增加CACHE类型
    /**
     * 同步消息类型
     */
    public static final byte DATA_SYN = 2;

    //2012-5-28
    //old MQ publish
    /**
     * 点对点通用消息类型
     */
    public static final byte P2P_TYPE_NOMAL = 0;

    //point to point
    /**
     * 点对点request消息类型
     */
    public static final  byte P2P_TYPE_REQUEST = 1;

    //Synchronize  message
    /**
     * 点对点reply消息类型
     */
    public static final byte P2P_TYPE_REPLY = 2;

    //p2p 消息的类型
    /**
     * 消息发送方式：没有确认，没有持久化
     */
    public static final byte NON_ACK_NON_PERSISTENT = 0;    //没有确认，没有持久化
    /**
     * 消息发送方式：没有确认，有持久化
     */
    public static final byte NON_ACK_HAS_PERSISTENT = 1;    //没有确认，有持久化
    /**
     * 消息发送方式：有确认，没有持久化
     */
    public static final byte HAS_ACK_NON_PERSISTENT = 2;    //有确认，没有持久化

    //DeliveryMode
    /**
     * 消息发送方式：有确认，有持久化
     */
    public static final byte HAS_ACK_HAS_PERSISTENT = 3;    //有确认，有持久化
    //UpdateType
    static final byte INSERT = 0;
    static final byte UPDATE = 1;
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Message.class);
    /**
     * 分组消息字段最大长度
     */
    public static int maxGroupFieldLength = Consts.MAX_GROUP_FIELD_LEN;
    //最大blob字段消息
    public static int maxBlobFieldLength = Consts.MAX_BLOB_FIELD_LEN;

    private int iMessageId;
    //private byte _nIsRedeliveried;
    //private byte _nCacheMessageID;
    //private byte _nIsInitCache;
    //private int _iCorrelationId;
    //private int _iTimeToLive;
    //private byte _nDeliveryMode;
    private byte nUpdateType;
    private byte nIsCachedMessage;
    //private MessageBody _oMessageBody;
    private BaseDestination oDestination;
    private BaseDestination oReplyTo;
    //p2p Message
    private String sServerID; //_sACSID;
    //客户端连接号
    private int connectionID;
    //数据类型
    private byte dataType;
    //消息发送标志
    private String signalID;
    //是否request或reply
    //0：正常数据；1：request; 2:reply
    private byte p2pType;

    /**
     * 构造函数
     */
    public Message()
    {
        super();
        iMessageId = 0;
        dataType = 0;
        nUpdateType = INSERT;
        nIsCachedMessage = 0;

        oDestination = new BaseDestination();
        oReplyTo = new BaseDestination();
        msgType = IMessage.MESSAGE_TYPE_DATA;
        //_oMessageBody = new MessageBody();

        sServerID = "";
        connectionID = -1;
        signalID = "";
        //_dataType=DATA_MQ;
        p2pType = P2P_TYPE_NOMAL;
        setInterMsgType(ConstsMessage.MSG_TYPE_DATA_PUB);
        //setP2PType((byte)0);
    }

    /**
     * 设置分组消息字段最大长度
     *
     * @param int nLen： 字段最大长度
     * @return void
     * @throws
     */
    public static void setMaxGroupFieldLength(int nLen)
    {
        maxGroupFieldLength = nLen;
    }

    /**
     * 设置Blob消息字段最大长度
     *
     * @param int nLen： 字段最大长度
     * @return void
     * @throws
     */
    public static void setMaxBlobFieldLength(int nLen)
    {
        maxBlobFieldLength = nLen;
    }

    /**
     * 生成新的reply消息
     *
     * @param null
     * @return 生成的reply消息
     * @throws
     */
    public Message createReplyMessage()
    {
        Message msg = new Message();
        msg.setMessageID(this.getMessageID());
        if (this.getReplyTo().getName() == null) {
            msg.setDestination(this.getDestination());
        }
        else {
            msg.setDestination(this.getReplyTo());
        }
        if (this.getSignalID() != null) {
            msg.setSignalID(this.getSignalID());
        }

        switch (getInterMsgType()) {
            case ConstsMessage.MSG_TYPE_DATA_REQ: {
                msg.setConnectionID(connectionID);
                msg.setSvrID(sServerID);
                msg.setDataType(DATA_P2P);

                msg.setInterMsgType(ConstsMessage.MSG_TYPE_DATA_REPLY);
                msg.setP2PType(Message.P2P_TYPE_REPLY);
            }
            break;
            case ConstsMessage.MSG_TYPE_DATA_P2P: {
                msg.setConnectionID(connectionID);
                msg.setSvrID(sServerID);
                msg.setDataType(DATA_P2P);

                msg.setInterMsgType(ConstsMessage.MSG_TYPE_DATA_P2P);
                msg.setP2PType(Message.P2P_TYPE_NOMAL);
            }
            break;
            case ConstsMessage.MSG_TYPE_DATA_PUB:
                break;

            default:
                logger.error("createReplyMessage unsupport message type:" + getInterMsgType());
                break;
        }
        return msg;
    }

    /**
     * 返回消息地址
     *
     * @param null
     * @return BaseDestination： 消息的地址
     * @throws
     */
    public BaseDestination getDestination()
    {
        return oDestination;
    }

    /**
     * 设置消息地址
     *
     * @param BaseDestination destination：消息地址
     * @return void
     * @throws
     */
    public void setDestination(BaseDestination destination)
    {
        oDestination = null;            //清除旧地址
        oDestination = destination;    //设置新地址
    }

    /**
     * 返回回应的消息地址
     *
     * @param null
     * @return BaseDestination： 消息的地址
     * @throws
     */
    public BaseDestination getReplyTo()
    {
        return oReplyTo;
    }

    /**
     * 设置回应消息地址
     *
     * @param BaseDestination replyTo： 回应的消息地址
     * @return void
     * @throws
     */
    public void setReplyTo(BaseDestination replyTo)
    {
        oReplyTo = null;
        oReplyTo = replyTo;

    }

    /**
     * 获取消息ID
     *
     * @param null
     * @return int： 消息ID号
     * @throws
     */
    public int getMessageID()
    {
        return iMessageId;
    }

    /**
     * 设置消息ID
     *
     * @param int messageId：消息ID号
     * @return void
     * @throws
     */
    public void setMessageID(int messageId)
    {
        iMessageId = messageId;
    }

    /**
     * 获取消息更新方式
     *
     * @param null
     * @return byte ：更新方式
     * @throws
     */
    public byte getUpdateType()
    {
        return nUpdateType;
    }

    /**
     * 设置消息更新方式
     *
     * @param byte updateType：更是方式
     * @return void
     * @throws
     */
    public void setUpdateType(byte updateType)
    {
        nUpdateType = updateType;
    }

    /**
     * 设置消息Cahce方式，内部使用
     *
     * @param byte isCachedMessage：Cache方式
     * @return void
     * @throws
     */
    @Deprecated
    public void isCachedMessage(byte isCachedMessage)
    {
        nIsCachedMessage = isCachedMessage;
    }

    /**
     * 获取消息更新方式,内部使用
     *
     * @param null
     * @return byte ：更新方式
     * @throws
     */
    @Deprecated
    public byte isCachedMessage()
    {
        return nIsCachedMessage;
    }

    /**
     * 获取数据类型
     *
     * @return byte
     * @throws
     */
    public byte getDataType()
    {
        return dataType;
    }

    /**
     * 设置消息数据类型
     *
     * @param byte dtype 消息数据类型
     * @return void
     * @throws
     */
    public void setDataType(byte dtype)
    {
        dataType = dtype;
    }

    /**
     * 获取连接ID
     *
     * @return int
     * @throws
     */
    public int getConnectionID()
    {
        return connectionID;
    }

    /**
     * 设置连接ID
     *
     * @param int connectionID 连接ID
     * @return void
     * @throws
     */
    public void setConnectionID(int connectionID)
    {
        this.connectionID = connectionID;
        dataType = DATA_P2P;
    }

    /**
     * 获取SvrID
     *
     * @return String
     * @throws
     */
    public String getSvrID()
    {
        return sServerID;
    }

    /**
     * 设置服务ID
     *
     * @param String serverID 服务ID
     * @return void
     * @throws
     */
    public void setSvrID(String serverID)
    {
        sServerID = serverID;
        dataType = DATA_P2P;
    }

    /**
     * 获取消息发送方标记ID
     *
     * @param
     * @return String： 消息发送方标记
     * @throws
     */
    public String getSignalID()
    {
        return signalID;
    }

    /**
     * 消息发送方标记ID
     *
     * @param String signalID 消息发送方标记
     * @return void
     * @throws
     */
    public void setSignalID(String signalID)
    {
        this.signalID = signalID;
    }

    /**
     * 获取p2p消息类型
     *
     * @param null
     * @return byte: p2p消息类型
     * @throws
     */
    public byte getP2PType()
    {
        return p2pType;
    }

    /**
     * 设置p2p消息类型
     *
     * @param byte bType： p2p消息类型
     * @return void
     * @throws
     */
    public void setP2PType(byte bType)
    {
        p2pType = bType;
    }
}

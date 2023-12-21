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

public abstract class IMessage
{
    /**
     * 消息类型名称
     */
    public static final String[] MESSAGE_TYPE = {"UNKNOWN", "CTL", "DATA", "ACK"};

    /**
     * 控制消息
     */
    public static final byte MESSAGE_TYPE_CTL = 1;    //控制消息

    /**
     * 数据消息
     */
    public static final byte MESSAGE_TYPE_DATA = 2;    //数据消息
    public byte msgType = 1;
    /**
     * 确认消息
     */
    //static final public byte MESSAGE_TYPE_ACK = 3;//确认消息
    //static final public byte MESSAGE_TYPE_ACS = 4;//ACS消息
    private byte nInterMsgType;
    private IMessage linkMessage = null;    //连接的下一个消息
    //保存消息数据流信息
    private byte[] btStream = null;
    private byte[] wsStream = null;
    //private int _iStream = 0;
    //private byte[] _webStream = null;
    //private int _webStreamLen=0;
    private MessageBody oMessageBody;

    public IMessage()
    {
        oMessageBody = new MessageBody();
    }

    /**
     * 获取消息类型
     *
     * @param null
     * @return byte ： 消息类型
     * @throws
     */
    public byte getInterMsgType()
    {
        return nInterMsgType;
    }

    /**
     * 设置消息类型
     *
     * @param byte nType： 消息类型
     * @return void
     * @throws
     */
    public void setInterMsgType(byte nType)
    {
        nInterMsgType = nType;
    }

    public byte getMessageType()
    {
        return msgType;
    }

    public void setMessageType(byte nType)
    {
        //m_msgType=nType;
    }

    /**
     * 获取消息内容
     *
     * @param byte[] btStream：消息内容
     * @return void
     * @throws
     */
    public byte[] getWsStream()
    {
        return wsStream;
    }

    /**
     * 设置消息内容
     *
     * @param byte[] btStream：消息内容
     * @return void
     * @throws
     */
    public void setWsStream(byte[] btStream)
    {
        wsStream = btStream;

    }

    /**
     * 获取消息内容
     *
     * @param byte[] btStream：消息内容
     * @return void
     * @throws
     */
    public byte[] getStream()
    {
        return btStream;
        //iStream = _iStream;
    }

    /**
     * 设置消息内容
     *
     * @param byte[] btStream：消息内容
     * @return void
     * @throws
     */
    public void setStream(byte[] btStream)
    {
        this.btStream = btStream;
        //_iStream = iStream;
    }

    //设置此消息连接的下一个消息
    public void linkMessage(IMessage linkMsg)
    {
        linkMessage = linkMsg;
    }

    //取得此消息连接的下一个消息
    public IMessage linkMessage()
    {
        return linkMessage;
    }

    /**
     * 获取消息内容结构body
     *
     * @param null
     * @return MessageBody ：消息内容结构
     * @throws
     */
    public MessageBody getMessageBody()
    {
        return oMessageBody;
    }

    /**
     * 设置消息内容结构body
     *
     * @param MessageBody oMessageBody：消息内容结构
     * @return
     * @throws
     */
    public void setMessageBody(MessageBody oMessageBody)
    {
        this.oMessageBody = null;
        this.oMessageBody = oMessageBody;
    }
}

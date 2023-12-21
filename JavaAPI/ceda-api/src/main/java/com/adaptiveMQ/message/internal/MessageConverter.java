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

package com.adaptiveMQ.message.internal;

import com.adaptiveMQ.client.internal.MessageCacheList;
import com.adaptiveMQ.message.BaseDestination;
import com.adaptiveMQ.message.ControlMessage;
import com.adaptiveMQ.message.IMessage;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.MessageAppend;
import com.adaptiveMQ.message.MessageBody;
import com.adaptiveMQ.utils.BufferByte;
import com.adaptiveMQ.utils.ConstsMessage;
import com.adaptiveMQ.utils.Utils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import sun.misc.BASE64Encoder;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class MessageConverter
{
    public static final String sKey = UUID.randomUUID().toString().substring(0, 8);
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageCacheList.class);
    private static final short MSG_VERSION = 0x2041; // 消息版本(2.0A)
    private static final Random random = new Random();
    private static final BASE64Encoder m_base64Encoder = new BASE64Encoder();

    private static final AtomicInteger replySid = new AtomicInteger(1);
    private static final String m_strDot = ".";

    private MessageConverter()
    {
    }

    public static String getWebSocketKey()
    {
        byte[] random = new byte[16];
        MessageConverter.random.nextBytes(random);
        return m_base64Encoder.encode(random);
    }

    public static int getMaskInt()
    {
        return random.nextInt();
    }

    public static byte[] wsInt2Byte(int nlen)
    {
        byte[] bret = new byte[2];
        bret[0] = (byte) ((nlen >> 8) & 0X00FF);
        bret[1] = (byte) (nlen & 0X00FF);
        return bret;
    }

    public static int wsByte2Int(byte[] bBytes)
    {
        int nlen = 0;
        nlen = (bBytes[0] << 8 & 0XFF00) | (bBytes[1] & 0X00FF);
        return nlen;
    }

    // 获取拆分后的消息
    public static Message[] getGroupMessage(BufferByte btBodyBuf, IMessage msg) throws Exception
    {
        Message[] mret = null;
        if (msg.getInterMsgType() > 90) {
            Message dmsg = (Message) msg;

            //if (dmsg.getDataType() == Message.DATA_P2P) {
            MessageAppend append = dmsg.getMessageBody().getAppend();
            if (append == null || append.getAppendType() != MessageAppend.MSG_GROUP) {
                return mret;
            }

            double dnum = (double) append.getDataLength() / (double) Message.maxGroupFieldLength;
            short nnum = (short) dnum;
            short num = nnum;
            if (Math.abs(dnum - nnum) > 0.000001) {
                num++;
            }
            boolean bFirst = true;
            byte[] bSignData = null;
            int nSplitlength = 0;
            short npos = append.getPosition();

            mret = new Message[num];

            for (short i = 0; i < num; i++) {
                byte[] bData = null;
                int nlength;
                if ((i + 1) < num) {
                    nlength = Message.maxGroupFieldLength;

                }
                else {
                    nlength = append.getDataLength() - nSplitlength;
                }

                // BufferByte btBodyBuf = new BufferByte();
                btBodyBuf.clear();
                btBodyBuf.putByte(MessageAppend.MSG_GROUP);

                Message nmsg;
                MessageBody nbody;
                if (bFirst) {
                    bFirst = false;
                    nmsg = dmsg;
                    nbody = nmsg.getMessageBody();
                    // copy all message；
                    nmsg.setReplyTo(dmsg.getReplyTo());
                    // dmsg.getMessageBody().copyRecord(dmsg.getMessageBody(),
                    // nbody);
                    // type+pos+num+length+AllLength
                    btBodyBuf.putShort(i);

                    btBodyBuf.putShort(num);
                    btBodyBuf.putInt(append.getDataLength());
                    btBodyBuf.putShort(npos);

                    btBodyBuf.putInt(nlength);

                }
                else {
                    nmsg = new Message();
                    nbody = nmsg.getMessageBody();
                    // 复制topic， type
                    nmsg.setInterMsgType(dmsg.getInterMsgType());
                    //nmsg.setDataType(dmsg.getDataType());
                    nmsg.setDestination(dmsg.getDestination());
                    //nmsg.setP2PType(dmsg.getP2PType());
                    nmsg.setConnectionID(dmsg.getConnectionID());

                    if (dmsg.getInterMsgType() == ConstsMessage.MSG_TYPE_DATA_REQ || dmsg.getInterMsgType() == ConstsMessage.MSG_TYPE_DATA_PUB_GROUP) {
                        nmsg.setReplyTo(dmsg.getReplyTo());
                    }

                    nmsg.setSvrID(dmsg.getSvrID());
                    // type+currnum+length
                    btBodyBuf.putShort(i);
                    btBodyBuf.putInt(nlength);
                }

                bSignData = btBodyBuf.convertBytes();

                bData = new byte[nlength];

                System.arraycopy(append.getBlobData(), nSplitlength, bData, 0, nlength);
                nSplitlength = nSplitlength + nlength;

                // set signal
                nbody.addBytes((short) 0, bSignData);
                // set data
                nbody.addBytes(npos, bData);

                mret[i] = nmsg;
                // btBodyBuf.clear();
                // btBodyBuf=null;
            }
            //}

        }
        return mret;
    }

    // 将消息转换为字节流，返回值为字节流长度
    public static byte[] msg2Byte(BufferByte btMsgBuf, BufferByte btBodyBuf, IMessage msg) throws Exception
    {
        if (msg == null) {
            throw new MessageConvertException("msg2Byte: IMessage is NULL");
        }

        short iCurPos = 0;

        byte nMessageType = 0;
        // BufferByte btMsgBuf = new BufferByte();
        byte[] btOutput = null;
        btMsgBuf.clear();
        btBodyBuf.clear();
        nMessageType = msg.getInterMsgType(); // 取得消息类型

        // 声明变量，供下面使用
        // ACKMessage ackMsg = null;
        ControlMessage ctlMsg = null;
        Message dataMsg = null;

        //如果是publish消息，还存在Messageappend,
        if (nMessageType == ConstsMessage.MSG_TYPE_DATA_PUB) {
            dataMsg = (Message) msg;
            if (msg.getMessageBody().getBlobField() != null) {
                nMessageType = ConstsMessage.MSG_TYPE_DATA_PUB_GROUP;
                String tdes = dataMsg.getDestination().getName() + m_strDot + sKey + m_strDot + replySid.getAndIncrement();
                if (tdes.length() > 200) {
                    throw new Exception("message topic too long");
                }
                msg.setInterMsgType(ConstsMessage.MSG_TYPE_DATA_PUB_GROUP);
                BaseDestination rdes = new BaseDestination(tdes);
                dataMsg.setReplyTo(rdes);
                if (replySid.get() > 500000) {
                    replySid.set(1);
                }
            }
        }
        // 消息类型
        btMsgBuf.putByte(nMessageType);
        iCurPos++;

        // 根据消息类型编码消息
        switch (nMessageType) {
            case ConstsMessage.MSG_TYPE_CTRL_HB:
            case ConstsMessage.MSG_TYPE_CTRL_SUB:
            case ConstsMessage.MSG_TYPE_CTRL_UNSUB:
            case ConstsMessage.MSG_TYPE_CTRL_LOGIN:
            case ConstsMessage.MSG_TYPE_CTRL_LOGOUT: // 控制消息: //控制消息
            {
                // 转换为CControlMessage
                ctlMsg = (ControlMessage) msg;

                if (nMessageType == ConstsMessage.MSG_TYPE_CTRL_HB) {
                    btOutput = btMsgBuf.convertBytes();
                    break;
                }
                // Control Code
                btMsgBuf.putByte(ctlMsg.getControlCode());
                iCurPos++;

                byte[] btBody = null;

                btBody = MessageBodyConverter.msg2Byte(btBodyBuf, ctlMsg.getMessageBody());

                if (btBody != null) { // 转换成功
                    btMsgBuf.put(btBody);
                    // nLength += btBody.length;
                }

                btOutput = btMsgBuf.convertBytes();
            }
            break;
            case ConstsMessage.MSG_TYPE_DATA_PUB: {
                // 转换为CMessage
                dataMsg = (Message) msg;

                // Topic
                // 类型Publish/Queue/Temp/Admin
                BaseDestination pTopic = dataMsg.getDestination();
                if (pTopic.getName() == null) {
                    throw new MessageConvertException("msg2Byte: Topic is NULL");
                }
                // 长度
                byte nTopicLen;
                byte[] bTopics = Utils.getSysByte(pTopic.getName());
                //nTopicLen = (byte) (pTopic.getName().getBytes().length);
                nTopicLen = (byte) bTopics.length;
                btMsgBuf.putByte(nTopicLen);
                iCurPos++;
                // 内容
                btMsgBuf.put(bTopics);
                iCurPos += nTopicLen;

                // 消息体
                byte[] btBody = null;

                btBody = MessageBodyConverter.msg2Byte(btBodyBuf, dataMsg.getMessageBody());

                if (btBody != null) { // 转换成功
                    btMsgBuf.put(btBody);
                    // nLength += btBody.length;
                }
                btOutput = btMsgBuf.convertBytes();

            }
            break;
            case ConstsMessage.MSG_TYPE_DATA_REQ: // 数据消息
            case ConstsMessage.MSG_TYPE_DATA_REPLY:
            case ConstsMessage.MSG_TYPE_DATA_P2P:
            case ConstsMessage.MSG_TYPE_DATA_PUB_GROUP: {
                // 转换为CMessage
                dataMsg = (Message) msg;

                // Topic
                // 类型Publish/Queue/Temp/Admin
                BaseDestination pTopic = dataMsg.getDestination();
                if (pTopic.getName() == null) {
                    throw new MessageConvertException("msg2Byte: Topic is NULL");
                }
                // 长度
                byte nTopicLen;
                byte[] bTopics = Utils.getSysByte(pTopic.getName());
                nTopicLen = (byte) (bTopics.length);
                btMsgBuf.putByte(nTopicLen);
                iCurPos++;
                // 内容
                btMsgBuf.put(bTopics);
                iCurPos += nTopicLen;

                // connid
                btMsgBuf.put(Utils.intToByte(dataMsg.getConnectionID(), 3));

                // set SvrID
                byte[] bSvrID = Utils.getSysByte(dataMsg.getSvrID());
                byte nSvrIDLen = (byte) (bSvrID.length);
                btMsgBuf.putByte(nSvrIDLen);
                iCurPos++;
                // 内容
                btMsgBuf.put(bSvrID);
                iCurPos += nSvrIDLen;

                // set signalID
                byte nSignalIDLen;
                byte[] bSignalID = Utils.getSysByte(dataMsg.getSignalID());
                nSignalIDLen = (byte) (bSignalID.length);
                btMsgBuf.putByte(nSignalIDLen);
                iCurPos++;
                // 内容
                btMsgBuf.put(bSignalID);
                iCurPos += nSignalIDLen;

                if (nMessageType == ConstsMessage.MSG_TYPE_DATA_REQ || nMessageType == ConstsMessage.MSG_TYPE_DATA_PUB_GROUP) {
                    // set reply topic
                    BaseDestination reply = dataMsg.getReplyTo();
                    byte[] bReply = Utils.getSysByte(reply.getName());
                    byte nReplyLen = (byte) (bReply.length);
                    btMsgBuf.putByte(nReplyLen);
                    iCurPos++;
                    // 内容
                    btMsgBuf.put(bReply);
                    iCurPos += nReplyLen;
                }

                // 消息体
                byte[] btBody = null;

                btBody = MessageBodyConverter.msg2Byte(btBodyBuf, dataMsg.getMessageBody());

                if (btBody != null) { // 转换成功
                    btMsgBuf.put(btBody);
                    // nLength += btBody.length;
                }
                btOutput = btMsgBuf.convertBytes();
            }
            break;

            default:
                logger.error("MessageConverter: msg2Byte Unknown message type[" + nMessageType + "]");
                break;
        }

        // btMsgBuf.clear();
        // btMsgBuf = null;

        return btOutput;
    }

    // 将字节流转换为消息，返回值为消息类型，以及头消息和总的消息数目
    public static StrByte2MsgOutput byte2VaildMsg(BufferByte btMsgBuf, byte[] btInput, int iInputLen) throws Exception
    {
        if (btInput == null) {
            throw new MessageConvertException("byte2Msg: btInput is NULL");
        }

        if (iInputLen < 3) {
            throw new MessageConvertException("byte2Msg: Not enough message body");
        }

        int iCurPos = 0;
        byte nMsgType;
        IMessage pHeadMsg = null; // 头消息，负责保存第一个消息内容，和下一条消息的头
        IMessage pCurMsg = null; // 当前消息，保存当前消息的指针
        int msgNumber = 0; // 记录已经处理的消息数
        int iVaildByte = 0; // 记录合法的字节数
        boolean bReturn = false;

        int iEnd;

        StrByte2MsgOutput msgOutput = new StrByte2MsgOutput();

        /*
         * byte[] bbout=new byte[iInputLen];
         * System.arraycopy(btInput,0,bbout,0,iInputLen); BufferByte bout=new
         * BufferByte(bbout); System.out.println("deal len:"+iInputLen);
         * System.out.println(Utils.displayBuffer(bout));
         */
        // System.out.println("deal len:"+iInputLen);
        btMsgBuf.clear();
        //btMsgBuf.put(btInput);
        btMsgBuf.put(btInput, 0, iInputLen);
        /*
        byte[] bshow=new byte[iInputLen];
        System.arraycopy(btInput,0,bshow,0,iInputLen);
        System.out.println("in: "+Utils.displayBuffer(bshow));
        */

        // while(iCurPos < iInputLen)
        while ((iInputLen - iVaildByte) > 2) {
            // 版本信息
            byte bSign = btMsgBuf.getByte();
            if (bSign != ConstsMessage.WEBSOCKET_BIN) {
                throw new MessageConvertException("Byte2VaildMsg: Version is wrong");
            }
            iCurPos++;

            byte bLen = btMsgBuf.getByte();
            iCurPos++;
            int nlen = 0;
            if (bLen < ConstsMessage.BYTE126) {
                nlen = bLen;
            }
            else if (bLen == ConstsMessage.BYTE126) {
                if ((iInputLen - iCurPos) < 126) {
                    bReturn = true;
                    break;
                }
                byte[] bBytes = new byte[2];
                //nlen = btMsgBuf.getUShort();
                btMsgBuf.get(bBytes);
                nlen = wsByte2Int(bBytes);
                iCurPos += 2;
            }
            else {
                throw new MessageConvertException("byte2Msg: length is wrong");
            }

            //if ((iCurPos + nlen) > btInput.length) {
            if ((iCurPos + nlen) > iInputLen) {
                bReturn = true;
                break;
            }
            // 消息结束位置
            iEnd = iCurPos + nlen;
            // 声明下面会用到的变量
            // ACKMessage ackMsg = null;

            // 消息类型
            nMsgType = btMsgBuf.getByte();
            iCurPos++;

            switch (nMsgType) {
                case ConstsMessage.MSG_TYPE_CTRL_HB:
                case ConstsMessage.MSG_TYPE_CTRL_SUB:
                case ConstsMessage.MSG_TYPE_CTRL_UNSUB:
                case ConstsMessage.MSG_TYPE_CTRL_LOGIN:
                case ConstsMessage.MSG_TYPE_CTRL_LOGOUT: // 控制消息
                {
                    // 判断剩余的字节数是否够本次处理，如果不够，则返回处理好的消息退出
                    if (pHeadMsg == null) { //如果是第一条消息
                        pHeadMsg = new ControlMessage();
                        pCurMsg = pHeadMsg;
                    }
                    else { // 如果是多于一条的消息
                        pCurMsg.linkMessage(new ControlMessage());
                        pCurMsg = pCurMsg.linkMessage();
                    }
                    pCurMsg.setInterMsgType(nMsgType);

                    if (nMsgType == ConstsMessage.MSG_TYPE_CTRL_HB || nMsgType == ConstsMessage.MSG_TYPE_CTRL_LOGOUT) {
                        iVaildByte = iCurPos;
                        msgNumber++;
                        continue;
                    }

                    if ((iInputLen - iCurPos) < 3) {
                        bReturn = true;
                        break;
                    }

                    ControlMessage ctlMsg = (ControlMessage) pCurMsg;

                    // Control Code
                    ctlMsg.setControlCode(btMsgBuf.getByte());
                    iCurPos++;

                    int nBodyLen = iEnd - iCurPos;
                    // body length must >4

                    if (nBodyLen < 4) {
                        bReturn = true;
                        break;
                    }
                    ctlMsg.setMessageBody(MessageBodyConverter.byte2Body(btMsgBuf, nBodyLen));

                    /*
                     * byte btCmd[] = new byte[nCmdLen]; btBodyBuf.get(btCmd);
                     * ctlMsg.setCommand(btCmd); iCurPos+=nCmdLen;
                     */
                    iCurPos = iEnd;
                    // 如果信息完整，则保存字节数
                    iVaildByte = iEnd;
                }
                break;

                case ConstsMessage.MSG_TYPE_DATA_PUB: {
                    // 判断剩余的字节数是否够本次处理，如果不够，则返回处理好的消息退出
                    if ((iInputLen - iCurPos) < 8) {
                        bReturn = true;
                        break;
                    }

                    if (pHeadMsg == null) { // 如果是第一条消息
                        pHeadMsg = new Message();
                        pCurMsg = pHeadMsg;
                    }
                    else { // 如果是多于一条的消息
                        pCurMsg.linkMessage(new Message());
                        pCurMsg = pCurMsg.linkMessage();
                    }

                    pCurMsg.setInterMsgType(nMsgType);

                    // 使用当前消息指针
                    Message dataMsg = (Message) pCurMsg;

                    int nMsgLen = iEnd - iCurPos;

                    if (nMsgLen < 0 || (iInputLen - iCurPos - nMsgLen) < 0) {
                        bReturn = true;
                        break;
                    }

                    // Topic
                    // 类型Publish/Queue/Temp/Admin
                    // byte nTopicType = btMsgBuf.getByte();
                    // iCurPos++;
                    // 长度
                    byte nTopicLen = btMsgBuf.getByte();
                    iCurPos++;
                    // 内容
                    byte[] btTopic = new byte[nTopicLen];
                    btMsgBuf.get(btTopic);
                    // System.arraycopy(btTopicTmp, 0, btTopic, 0, nTopicLen-1);
                    // dataMsg.getDestination().setType(nTopicType);//设置Topic
                    //dataMsg.getDestination().setName(new String(btTopic));
                    dataMsg.getDestination().setName(Utils.getSysString(btTopic));
                    iCurPos += nTopicLen;
                    btTopic = null;

                    //性能测试时，注释
                    //*
                    // 消息体
                    int nBodyLen = iEnd - iCurPos;
                    // byte btBuffer[] = null;
                    // byte btBody[] = null;

                    // 获得消息体的长度(需要把short变为unsigned short)

                    if (iInputLen < (nBodyLen + iCurPos)) { // 消息内容的不可能比总消息内容大
                        throw new MessageConvertException("byte2Msg: MessageRecord size is bigger(" + nBodyLen + ") than input(" + iInputLen + ")");
                    }

                    dataMsg.setMessageBody(MessageBodyConverter.byte2Body(btMsgBuf, nBodyLen));
                    //*/
                    // btBody = null;
                    // btBuffer = null;
                    iCurPos = iEnd;
                    // 如果信息完整，则保存字节数
                    iVaildByte = iEnd;

                }
                break;
                case ConstsMessage.MSG_TYPE_DATA_REQ:
                case ConstsMessage.MSG_TYPE_DATA_REPLY:
                case ConstsMessage.MSG_TYPE_DATA_P2P:
                case ConstsMessage.MSG_TYPE_DATA_PUB_GROUP: {
                    // 判断剩余的字节数是否够本次处理，如果不够，则返回处理好的消息退出
                    if ((iInputLen - iCurPos) < 16) {
                        bReturn = true;
                        break;
                    }

                    if (pHeadMsg == null) { // 如果是第一条消息
                        pHeadMsg = new Message();
                        pCurMsg = pHeadMsg;
                    }
                    else { // 如果是多于一条的消息
                        pCurMsg.linkMessage(new Message());
                        pCurMsg = pCurMsg.linkMessage();
                    }

                    pCurMsg.setInterMsgType(nMsgType);
                    // 使用当前消息指针
                    Message dataMsg = (Message) pCurMsg;

                    int nMsgLen = iEnd - iCurPos;

                    if (nMsgLen < 0 || (iInputLen - iCurPos - nMsgLen) < 0) {
                        bReturn = true;
                        break;
                    }
                    // 如果信息完整，则保存字节数
                    iVaildByte = iEnd;
                    dataMsg.setDataType(Message.DATA_P2P);

                    // Topic
                    // 类型Publish/Queue/Temp/Admin
                    // byte nTopicType = btMsgBuf.getByte();
                    // iCurPos++;
                    // 长度
                    byte nTopicLen = btMsgBuf.getByte();
                    iCurPos++;
                    // 内容
                    byte[] btTopic = new byte[nTopicLen];
                    btMsgBuf.get(btTopic);
                    iCurPos += nTopicLen;
                    // System.arraycopy(btTopicTmp, 0, btTopic, 0, nTopicLen-1);
                    // dataMsg.getDestination().setType(nTopicType);//设置Topic
                /*
                if (nMsgType == ConstsMessage.MSG_TYPE_DATA_REPLY) {
                    dataMsg.getDestination().setType(BaseDestination.TEMP);
                    //dataMsg.setP2PType(Message.P2P_TYPE_REPLY);
                }else{
                    //dataMsg.setP2PType(Message.P2P_TYPE_REQUEST);
                }
                */
                    dataMsg.getDestination().setName(Utils.getSysString(btTopic));
                    btTopic = null;

                    // connid
                    byte[] bConnid = new byte[3];
                    btMsgBuf.get(bConnid);
                    int connID = Utils.byteToInt(bConnid, 3);
                    dataMsg.setConnectionID(connID);
                    iCurPos += 3;
                    bConnid = null;

                    // SvrID
                    // 长度
                    byte nSvrLen = btMsgBuf.getByte();
                    iCurPos++;
                    // 内容
                    byte[] btSvr = new byte[nSvrLen];
                    btMsgBuf.get(btSvr);
                    dataMsg.setSvrID(Utils.getSysString(btSvr));
                    iCurPos += nSvrLen;
                    btSvr = null;

                    // SignalID
                    // 长度
                    byte nSidLen = btMsgBuf.getByte();
                    iCurPos++;
                    // 内容
                    byte[] btSid = new byte[nSidLen];
                    btMsgBuf.get(btSid);
                    dataMsg.setSignalID(Utils.getSysString(btSid));
                    iCurPos += nSidLen;
                    btSid = null;

                    if (nMsgType == ConstsMessage.MSG_TYPE_DATA_REQ || nMsgType == ConstsMessage.MSG_TYPE_DATA_PUB_GROUP) {
                        dataMsg.setP2PType(Message.P2P_TYPE_REQUEST);
                        // SignalID
                        // 长度
                        byte nRTopicLen = btMsgBuf.getByte();
                        iCurPos++;
                        // 内容
                        byte[] btRTopic = new byte[nRTopicLen];
                        btMsgBuf.get(btRTopic);
                        //dataMsg.getReplyTo().setType(BaseDestination.TEMP);
                        dataMsg.getReplyTo().setName(Utils.getSysString(btRTopic));
                        iCurPos += nRTopicLen;
                        btRTopic = null;

                    }
                    else {
                        if (nMsgType == ConstsMessage.MSG_TYPE_DATA_P2P) {
                            dataMsg.setP2PType(Message.P2P_TYPE_NOMAL);
                        }
                        else {
                            dataMsg.setP2PType(Message.P2P_TYPE_REPLY);
                        }

                    }

                    // 获得消息体的长度
                    int nBodyLen = iEnd - iCurPos;
                    // byte btBuffer[] = null;
                    // byte btBody[] = null;

                    // 获得消息体的长度(需要把short变为unsigned short)

                    if (iInputLen < iEnd || nBodyLen < 0) { // 消息内容的不可能比总消息内容大
                        throw new MessageConvertException("byte2Msg: MessageRecord size is bigger(" + nBodyLen + ") than input(" + iInputLen + ")");
                    }

                    dataMsg.setMessageBody(MessageBodyConverter.byte2Body(btMsgBuf, nBodyLen));
                    // btBody = null;
                    // btBuffer = null;
                    iCurPos = iEnd;
                    // 如果信息完整，则保存字节数
                    iVaildByte = iEnd;

                }
                break;
                default:
                    msgNumber = 0;
                    logger.error("MessageConverter: byte2Msg Unknown message type[" + nMsgType + "]");
                    break;
            }
            if (bReturn) {
                break;
            }
            // 分析的消息数目加1
            msgNumber++;
        }

        // btMsgBuf.clear();
        // btBodyBuf = null;

        msgOutput.iMessageNumber = msgNumber;
        msgOutput.oMessage = pHeadMsg;
        msgOutput.iVaildByte = iVaildByte;

        return msgOutput;
    }

    public static byte[] getWsStream(byte[] btOutput, BufferByte buffByte) throws Exception
    {
        buffByte.clear();
        // int nlen=0;
        buffByte.putByte(ConstsMessage.WEBSOCKET_BIN);
        // nlen++;
        int sizebytes = btOutput.length < 126 ? 1 : 2;
        switch (sizebytes) {
            case 1: {
                buffByte.putByte((byte) (btOutput.length));
                // nlen++;
            }
            break;
            case 2: {
                buffByte.putByte(ConstsMessage.BYTE126);
                byte[] bBlen = MessageConverter.wsInt2Byte(btOutput.length);
                buffByte.put(bBlen);
                //buffByte.putShort((short) btOutput.length);
                // nlen +=3;
            }

            break;
            default: {
                logger.error("output length is larget:" + btOutput.length);
                return null;
            }
        }

        buffByte.put(btOutput);

        byte[] bout = buffByte.convertBytes();
        return bout;
    }

    public static class StrByte2MsgOutput
    {
        public IMessage oMessage; // 消息头
        public int iMessageNumber; // 消息的个数
        public int iVaildByte;
    }
}

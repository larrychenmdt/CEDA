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

import com.adaptiveMQ.message.MessageBody;
import com.adaptiveMQ.message.MessageRecord;
import com.adaptiveMQ.message.MessageRecord.CField;
import com.adaptiveMQ.utils.BufferByte;
import com.adaptiveMQ.utils.Consts;
import com.adaptiveMQ.utils.Utils;

import java.util.Iterator;

public final class MessageBodyConverter
{
    private MessageBodyConverter() {}

    //MessageBody转换为数据流
    public static byte[] msg2Byte(BufferByte btBodyBuf, MessageBody oMessageBody) throws Exception
    {
        if (oMessageBody == null) {
            throw new MessageConvertException("Msg2Byte: MessageBody is NULL");
        }

        int iCurPos = 0;    //当前输出指针所指的位置
        btBodyBuf.clear();
        //转换消息内容
        //Enumeration it = oMessageBody.getValues();
        Iterator<CField> it = oMessageBody.getFieldIterator();

        //转换消息内容
        //iCurPos = 2;//除去消息字节数2，然后开始储存内容

        //首先转换不包含Group的域
        while (it.hasNext()) {
            CField oField = it.next();

            //检查消息体是否超过规定大小
            if (iCurPos - 4 >= Consts.MAX_MESSAGE_BODY_LENGTH) {
                throw new MessageConvertException("Msg2Byte: Message Body(" + iCurPos + ") is too large");
            }

            //FID(12位) 字段类型(3位) Group分隔符(1位)
            short nFieldHead = (short) ((((oField.nPosition << 4) & 0xFFF0) | ((oField.nFieldType) << 1) & 0x000E) & 0xFFFE);
            btBodyBuf.putShort(nFieldHead);
            iCurPos += 2;

            switch (oField.nFieldType) {
                case MessageRecord.INT:
                    btBodyBuf.putInt(((Integer) oField.oField).intValue());
                    break;
                case MessageRecord.LONG:
                    btBodyBuf.putLong(((Long) oField.oField).longValue());
                    break;
                case MessageRecord.SHORT:
                    btBodyBuf.putShort(((Short) oField.oField).shortValue());
                    break;
                case MessageRecord.FLOAT:
                    btBodyBuf.putFloat(((Float) oField.oField).floatValue());
                    break;
                case MessageRecord.DOUBLE:
                    btBodyBuf.putDouble(((Double) oField.oField).doubleValue());
                    break;
                case MessageRecord.STRING:
                    //byte[] btString = ((String)oField.oField + (char)0).getBytes();
                    //byte[] btString = ((String)oField.oField).getBytes();
                    byte[] btString = Utils.getSysByte((String) oField.oField);
                    btBodyBuf.putShort((short) btString.length);    //字段长度(占2字节)
                    iCurPos += 2;
                    btBodyBuf.put(btString);
                    break;
                case MessageRecord.BYTES:
                    btBodyBuf.putShort((short) ((byte[]) oField.oField).length);    //字段长度(占2字节)
                    iCurPos += 2;
                    btBodyBuf.put((byte[]) oField.oField);
                    break;
            }

            iCurPos += oField.iLen;
        }

        //保存消息
        //byte btMsg[] = new byte[btBodyBuf.size()];

        //System.arraycopy(btBodyBuf.convertBytes(),0,btMsg,0,btBodyBuf.size());//消息体

        byte[] btMsg = btBodyBuf.convertBytes();
        //System.out.println(Utils.displayBuffer(btMsg));
        return btMsg;
    }

    //将数据流转换为CMessageBody
    public static MessageBody byte2Body(BufferByte btBodyBuf, int iLen) throws Exception
    {
        //short iLen = 0;//消息体的长度
        //int iLen = 0;//消息体的长度
        MessageBody oMessageBody = null; //返回的消息体

        //消息内容
        int nLen;            //占用字节数
        short nFieldHead = 0;
        short nPosition;    //所在位置
        byte nFieldType;    //字段类型
        //int nGroupSeparator = 0;//是否是一个Group的分隔符
        //int nGroupBegin = 0;//是一个Group的开始1还是结束2
        int iCurPos = 0;        //内容除去消息体长度2
        MessageRecord oRecord = null;    //消息数据指针，可以指向消息体也可以指向Group
        //MessageRecord oGroup = null;//保存Group

        oMessageBody = new MessageBody();
        oRecord = oMessageBody;
        //去掉CRC32的4个字节
        //iLen -=4;
        while (iCurPos < iLen) {
            //去掉CRC32的4个字节
            //FID(12位) 字段类型(3位) Group分隔符(1位)
            nFieldHead = btBodyBuf.getShort();
            iCurPos += 2;

            //分解标示符
            nPosition = (short) ((nFieldHead >> 4) & 0x0FFF);
            nFieldType = (byte) ((nFieldHead >> 1) & 0x0007);

            switch (nFieldType) {
                case MessageRecord.INT:
                    oRecord.addInt(nPosition, btBodyBuf.getInt());
                    iCurPos += 4;
                    break;
                case MessageRecord.LONG:
                    oRecord.addLong(nPosition, btBodyBuf.getLong());
                    iCurPos += 8;
                    break;
                case MessageRecord.SHORT:
                    oRecord.addShort(nPosition, btBodyBuf.getShort());
                    iCurPos += 2;
                    break;
                case MessageRecord.FLOAT:
                    oRecord.addFloat(nPosition, btBodyBuf.getFloat());
                    iCurPos += 4;
                    break;
                case MessageRecord.DOUBLE:
                    oRecord.addDouble(nPosition, btBodyBuf.getDouble());
                    iCurPos += 8;
                    break;
                case MessageRecord.STRING:
                    nLen = btBodyBuf.getUShort();
                    iCurPos += 2;

                    if (nLen > Consts.MAX_MESSAGE_BODY_LENGTH) {
                        throw new MessageConvertException("Byte2Msg: String field length(" + nLen + ") cannot larger than " + Consts.MAX_MESSAGE_BODY_LENGTH);
                    }

                    if (nLen > 0) {
                        byte[] btBuf = new byte[nLen];

                        //byte[] _btChar = new byte[nLen-1];

                        btBodyBuf.get(btBuf);
                        //System.arraycopy(_btBuf,0,_btChar,0,nLen-1);//在Java中的字符串不需要已'0'结尾
                        //_strString = new String(_btBuf);
                        String strString = Utils.getSysString(btBuf);
                        oRecord.addString(nPosition, strString);
                        btBuf = null;
                        //_btChar = null;
                    }
                    iCurPos += nLen;
                    break;
                case MessageRecord.BYTES:
                    nLen = btBodyBuf.getUShort();
                    iCurPos += 2;

                    if (nLen > Consts.MAX_MESSAGE_BODY_LENGTH) {
                        throw new MessageConvertException("Byte2Msg: Bytes field length(" + nLen + ") cannot larger than " + Consts.MAX_MESSAGE_BODY_LENGTH);
                    }

                    if (nLen > 0) {
                        byte[] btBuf = new byte[nLen];
                        btBodyBuf.get(btBuf);
                        oRecord.addBytes(nPosition, btBuf);
                    }
                    iCurPos += nLen;
                    break;

            }
        }
        return oMessageBody;
    }
}

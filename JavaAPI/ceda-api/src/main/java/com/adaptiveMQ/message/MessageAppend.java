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

public class MessageAppend
{
    /**
     * 分组消息
     */
    public static final byte MSG_GROUP = 1;
    //type
    private byte type = 0;

    private int msgLength;

    private short nPosition;

    private byte[] data = null;

    /**
     * 获取扩展类型
     *
     * @param null
     * @return byte:扩展类型
     * @throws
     */
    public byte getAppendType()
    {
        return type;
    }

    /**
     * 获取扩展内容
     *
     * @param null
     * @return byte[]:扩展内容
     * @throws
     */
    public byte[] getBlobData()
    {
        return data;
    }

    /**
     * 获取扩展内容长度
     *
     * @param null
     * @return int:扩展内容长度
     * @throws
     */
    public int getDataLength()
    {
        return msgLength;
    }

    /**
     * 获取扩展内容位置
     *
     * @param null
     * @return short:扩展内容位置
     * @throws
     */
    public short getPosition()
    {
        return nPosition;
    }

    /**
     * 设置扩展消息内容
     *
     * @param short nPosition：位置, byte[] bdata： 扩展内容
     * @return void
     * @throws
     */
    public void setBlobField(short nPosition, byte[] bdata) throws MessageBodyException
    {
        if (bdata.length > Message.maxBlobFieldLength) {
            throw new MessageBodyException("Data too large");
        }
        msgLength = bdata.length;
        data = bdata;
        this.nPosition = nPosition;
        type = MessageAppend.MSG_GROUP;

    }
}

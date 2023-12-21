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

package com.adaptiveMQ.client.internal;

import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.utils.BufferByte;
import com.adaptiveMQ.utils.ConstsMessage;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.HashMap;

class GroupProcesser
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(GroupProcesser.class);
    private HashMap<String, GroupData> hashGroup;
    private IMessageProcessor processor = null;
    private BufferByte msgBuf;

    public GroupProcesser(IMessageProcessor msgProcessor)
    {
        processor = msgProcessor;
        msgBuf = new BufferByte();
        hashGroup = new HashMap<String, GroupData>();
    }

    public void close()
    {
        hashGroup.clear();
        hashGroup = null;
        msgBuf.close();
        msgBuf = null;
    }

    public void addMessage(byte[] bvalue, Message msg)
    {
        msgBuf.clear();
        msgBuf.put(bvalue);
        //get type
        msgBuf.get();
        //get pos

        String skey = msg.getDestination().getName();
        if (msg.getInterMsgType() == ConstsMessage.MSG_TYPE_DATA_PUB_GROUP) {
            skey = msg.getReplyTo().getName();
        }

        short ncurNum = msgBuf.getShort();
        GroupData groupDef = null;
        if (ncurNum == 0) {
            short num = msgBuf.getShort();
            int nAllLength = msgBuf.getInt();
            short npos = msgBuf.getShort();
            groupDef = new GroupData(processor);
            groupDef.number = num;
            groupDef.nAllLength = nAllLength;
            groupDef.npos = npos;
            hashGroup.put(skey, groupDef);

        }
        else {
            groupDef = hashGroup.get(skey);
            //groupDef=m_hashGroup.get(skey);
        }
        if (groupDef == null) {
            logger.error("can't find group, key=" + skey);
            return;
        }

        int nlength = msgBuf.getInt();
        groupDef.onMessage(ncurNum, nlength, msg);
        if (ncurNum == (groupDef.number - 1)) {
            groupDef.groupMessage();
            hashGroup.remove(skey);
        }
    }
}

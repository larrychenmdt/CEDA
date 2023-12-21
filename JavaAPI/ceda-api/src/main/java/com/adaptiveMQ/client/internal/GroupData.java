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
import com.adaptiveMQ.message.MessageBody;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;

class GroupData
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(GroupData.class);
    private final ArrayList<GroupValue> groupValues;
    short npos;
    short number;
    int nAllLength;
    private Message firstMsg;
    private IMessageProcessor messageProcessor = null;

    public GroupData(IMessageProcessor msgProcessor)
    {
        groupValues = new ArrayList<>();
        messageProcessor = msgProcessor;
    }

    public void onMessage(short currNum, int nlength, Message msg)
    {
        MessageBody body = msg.getMessageBody();
        if (currNum == 0) {
            firstMsg = msg;
            body.removeField((short) 0);
        }

        try {
            byte[] bdata = body.getBytes(npos);
            GroupValue gvalue = new GroupValue();
            gvalue.bdata = bdata;
            gvalue.currNum = currNum;
            gvalue.nlength = nlength;
            groupValues.add(gvalue);

        }
        catch (Exception e) {
            logger.error("add message  failed", e);
        }
    }

    public int groupMessage()
    {
        int nret = -1;
        if (groupValues.size() == number) {
            byte[] bdata = new byte[nAllLength];
            int nlen = 0;
            short currNum = 0;
            while (!groupValues.isEmpty()) {
                GroupValue value = groupValues.remove(0);
                if (currNum != value.currNum) {
                    logger.error("value.curNum=" + value.currNum + ", curNum=" + currNum);
                    break;
                }
                System.arraycopy(value.bdata, 0, bdata, nlen, value.nlength);
                nlen = nlen + value.nlength;
                currNum++;
            }
            if (nlen == nAllLength) {
                try {
                    firstMsg.getMessageBody().setBlobField(npos, bdata);
                    messageProcessor.addInMessage(firstMsg);
                    nret = 0;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    logger.error("m_processor failed to addInMessage", e);
                }
            }
            else {
                logger.error("AllLength=" + nAllLength + ", data Length=" + nlen);
            }
        }
        groupValues.clear();
        return nret;
    }

    class GroupValue
    {
        short currNum;
        int nlength;
        byte[] bdata;
    }
}

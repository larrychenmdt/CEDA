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

package com.adaptiveMQ.server.internal;

import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.MessageBody;
import com.adaptiveMQ.message.MessageRecord;
import com.adaptiveMQ.message.MessageRecord.CField;
import com.adaptiveMQ.message.internal.MessageConverter;
import com.adaptiveMQ.utils.BufferByte;
import com.adaptiveMQ.utils.Utils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public final class CMessageCacheHandler
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CMessageCacheHandler.class);

    private final BufferByte msgBuf;
    private final BufferByte bodyBuf;
    private Hashtable<String, Message> messageMap = null;

    public CMessageCacheHandler()
    {
        messageMap = new Hashtable<String, Message>();
        msgBuf = new BufferByte();
        bodyBuf = new BufferByte();
    }

    public void addMessage(Message msg)
    {
        if (msg.isCachedMessage() == 0) {
            msg = null;
            return;
        }
        if (msg.getDataType() == Message.DATA_MQ) {
            Message omsg = messageMap.get(msg.getDestination().getName());
            if (omsg == null) {
                messageMap.put(msg.getDestination().getName(), msg);
                msg.setUpdateType(Message.CACHE);
                //System.out.println("catche msg :"+msg.getDestination().getName());
                //msg.setStream(null, 0);
            }
            else {
                MessageBody obody = omsg.getMessageBody();
                //Enumeration<MessageRecord.CField> enumBody=msg.getMessageBody().getValues();
                //添加到原有的消息中
                Iterator<CField> it = msg.getMessageBody().getFieldIterator();
                while (it.hasNext()) {
                    MessageRecord.CField field = it.next();
                    try {
                        //obody.removeField(field.nPosition);
                        obody.addField(field.nPosition, field);
                    }
                    catch (Exception e) {
                        logger.error(e);
                    }
                }
            }
        }

    }

    public List<Message> getMessage(String stopic)
    {
        ArrayList<Message> list = new ArrayList<Message>();

        if (Utils.isWildcard(stopic)) {
            Collection<Message> coll = messageMap.values();
            for (Message msg : coll) {
                if (Utils.matchDestination(stopic, msg.getDestination().getName())) {
                    //generate byte
                    messageClear(list, msg);

                }
            }
        }
        else {
            Message msg = messageMap.get(stopic);
            if (msg != null) {
                messageClear(list, msg);
            }
        }

        return list;
    }

    private void messageClear(ArrayList<Message> list, Message msg)
    {
        msg.setStream(null);
        msg.setWsStream(null);

        msgBuf.clear();
        bodyBuf.clear();

        try {
            byte[] bytes = MessageConverter.msg2Byte(msgBuf, bodyBuf, msg);
            msg.setStream(bytes);
            list.add(msg);

        }
        catch (Exception e) {
            logger.error(e);
        }
    }
}

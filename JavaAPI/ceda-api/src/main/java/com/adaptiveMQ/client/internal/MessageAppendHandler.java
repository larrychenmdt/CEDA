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
import com.adaptiveMQ.message.MessageAppend;
import com.adaptiveMQ.message.MessageBody;
import com.adaptiveMQ.message.MessageRecord;
import com.adaptiveMQ.utils.ConstsMessage;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class MessageAppendHandler
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageAppendHandler.class);

    private final IMessageProcessor process;

    private GroupProcesser groupHandler;

    public MessageAppendHandler(IMessageProcessor msgProcessor)
    {
        process = msgProcessor;
        groupHandler = new GroupProcesser(process);
    }

    public void close()
    {
        groupHandler.close();
        groupHandler = null;
    }

    public void addMessage(Message msg)
    {
        byte bType = msg.getInterMsgType();
        if (bType > ConstsMessage.MSG_TYPE_DATA_PUB) {
            MessageBody body = msg.getMessageBody();
            if (body.haseField((short) 0) != MessageRecord.BYTES) {
                process.addInMessage(msg);
            }
            else {
                try {
                    byte[] bvalue = body.getBytes((short) 0);
                    if (bvalue[0] == MessageAppend.MSG_GROUP) {
                        groupHandler.addMessage(bvalue, msg);
                    }
                    else {
                        logger.error("No AppendHandler for " + bvalue[0]);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    logger.error(e);
                }
            }
        }
        else {
            process.addInMessage(msg);
        }
    }
}

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

import com.adaptiveMQ.client.transport.IClientWriteService;
import com.adaptiveMQ.message.IMessage;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public final class MessageProcessorOut
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageProcessorOut.class);

    private IClientWriteService clientWrite = null;

    public MessageProcessorOut(MessageProcessor processor)
    {
    }

    public void setWriter(IClientWriteService clientWrite)
    {
        this.clientWrite = clientWrite;
    }

    //增加一个新的消息
    public int addMessage(IMessage msg)
    {
        if (clientWrite != null && msg != null) {
            clientWrite.addMessage(msg);
            return 1;
        }
        else {
            logger.error("MessageProcessorOut: addMessage: Cannot add message");
            return 0;
        }
    }
}

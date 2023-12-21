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

package com.adaptiveMQ.client.transport.tcp;

import com.adaptiveMQ.client.internal.MessageProcessor;
import com.adaptiveMQ.client.transport.AbstractWriteService;
import com.adaptiveMQ.message.internal.MessageConverter;
import com.adaptiveMQ.utils.BufferByte;
import com.adaptiveMQ.utils.ConstsMessage;
import com.adaptiveMQ.utils.TypeConverter;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.OutputStream;

public class OutputStreamWriteService extends AbstractWriteService
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(OutputStreamWriteService.class);
    private final String outFormat = "OutputStreamWriteService send bytes[%d], count[%d]";
    private final BufferByte buffByte = new BufferByte();
    private OutputStream oOutput = null;
    private MessageProcessor oMsgProcessor = null;
    private boolean mask = false;

    public OutputStreamWriteService(MessageProcessor msgProcessor)
    {
        super(msgProcessor);
        oMsgProcessor = msgProcessor;
        //m_oQueue=new ConcurrentLinkedQueue<IMessage>();
    }

    public void setStream(OutputStream outstream)
    {
        oOutput = outstream;
    }

    public void setMask()
    {
        mask = true;
    }

    public void output(byte[] btOutput, int size) throws Exception
    {
        buffByte.clear();
        // int nlen=0;
        buffByte.putByte(ConstsMessage.WEBSOCKET_BIN);
        // nlen++;
        int sizebytes = btOutput.length < 126 ? 1 : 2;

        if (mask) {
            switch (sizebytes) {
                case 1: {
                    buffByte.putByte((byte) (btOutput.length | ConstsMessage.NEGATIVE_BYTE128));
                    // nlen++;
                }
                break;
                case 2: {
                    buffByte.putByte((byte) (ConstsMessage.BYTE126 | ConstsMessage.NEGATIVE_BYTE128));
                    byte[] bBlen = MessageConverter.wsInt2Byte(btOutput.length);
                    buffByte.put(bBlen);
                    //m_buffByte.putShort((short) btOutput.length);
                    // nlen +=3;
                }

                break;
                default: {
                    logger.error("output length is larget:" + btOutput.length);
                    return;
                }
            }

            int nmask = MessageConverter.getMaskInt();
            byte[] bt4 = TypeConverter.int2byte(nmask);
            buffByte.put(bt4);
            // nlen +=4;
            for (int i = 0; i < btOutput.length; i++) {
                buffByte.putByte((byte) (btOutput[i] ^ bt4[i % 4]));
            }
        }
        else {
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
                    //m_buffByte.putShort((short) btOutput.length);
                    // nlen +=3;
                }

                break;
                default: {
                    logger.error("output length is larget:" + btOutput.length);
                    return;
                }
            }

            buffByte.put(btOutput);
        }

        byte[] bout = buffByte.convertBytes();
        //System.out.println("out len:"+bout.length);
        logger.debug(String.format(outFormat, bout.length, size));
        oOutput.write(bout);
    }

    @Override
    public void initWrite()
    {
    }

    @Override
    public void destory() {}
}

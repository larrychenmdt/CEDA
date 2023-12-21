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

package com.adaptiveMQ.client.transport;

import com.adaptiveMQ.client.internal.MessageAppendHandler;
import com.adaptiveMQ.client.internal.MessageProcessor;
import com.adaptiveMQ.message.IMessage;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.internal.MessageConverter;
import com.adaptiveMQ.utils.BufferByte;
import com.adaptiveMQ.utils.Consts;
import com.adaptiveMQ.utils.ConstsMessage;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

public final class ClientRecvDataBuffer extends Thread
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClientRecvDataBuffer.class);
    private final BufferByte msgBuf;
    private final BufferByte bodyBuf;
    //private LinkedBlockingQueue<byte[]> m_vecData = null;
    private final byte[] lastBuffer = new byte[Consts.MAX_RECEIVE_LEN]; //保存上次接收的不完整的数据

    //private Object obj = new Object();
    private final String inFormat = "ClientRecvDataBuffer add bytes[%d], count[%d]";
    private final byte[] buffer2X = new byte[Consts.MAX_RECEIVE_2X_LEN];
    private MessageProcessor oMsgProcessor = null;
    private LinkedBlockingQueue<byte[]> vecData = null;
    private boolean bRun = true;
    private MessageAppendHandler appendHandler = null;

    //private byte[] m_bdata= new byte[Consts.MAX_RECEIVE_LEN];
    private int iLastBuffer; //保存上次接收的不完整的数据长度

    public ClientRecvDataBuffer(MessageProcessor msgProcessor)
    {
        oMsgProcessor = msgProcessor;
        vecData = new LinkedBlockingQueue<byte[]>();
        appendHandler = new MessageAppendHandler(msgProcessor);
        msgBuf = new BufferByte();
        bodyBuf = new BufferByte();
        iLastBuffer = 0;
        //m_vecData = new Vector<byte[]>();
    }

    public void close()
    {
        bRun = false;
        this.interrupt();
        appendHandler.close();
        //m_appendHandler=null;
        msgBuf.close();
        //m_msgBuf=null;
        bodyBuf.close();
        //m_bodyBuf=null;
    }

    public void addData(byte[] bbdata)
    {
        vecData.add(bbdata);

        // logger.debug(String.format(inFormat, bbdata.length, vecData.size()));
    }

    public void run()
    {
        while (bRun) {
            byte[] buffer = null;
            try {
                buffer = vecData.take();
            }
            catch (Exception e) {
                logger.error("" + e.getMessage());
            }

            if (buffer == null) {
                continue;
            }

            if (!bRun) {
                break;
            }

            int iRecSize = buffer.length;
            //CLog.debug(m_take_format,iRecSize);
            MessageConverter.StrByte2MsgOutput result = null;

            try {
                int ndeal = iLastBuffer + iRecSize;
                if (ndeal < 3) {
                    //拷贝消息到lastbuffer
                    System.arraycopy(buffer, 0, lastBuffer, iLastBuffer, iRecSize);
                    iLastBuffer = ndeal;
                    continue;
                }

                //将上次的剩余数据加入
                if (iLastBuffer > 0) {
                    System.arraycopy(lastBuffer, 0, buffer2X, 0, iLastBuffer);
                }

                //加入本次数据
                System.arraycopy(buffer, 0, buffer2X, iLastBuffer, iRecSize);

                //计算合法的数据
                result = MessageConverter.byte2VaildMsg(msgBuf, buffer2X, ndeal);
                int iVaildSize = result.iVaildByte;

                //System.out.println("iVaildSize: "+iVaildSize);
                //将剩余数据保存
                iLastBuffer = iLastBuffer + iRecSize - iVaildSize;

                if (iLastBuffer > 0) { //复制剩余数据
                    System.arraycopy(buffer2X, iVaildSize, lastBuffer, 0, iLastBuffer);
                }

                if (iVaildSize < 1) {
                    continue;
                }

            }
            catch (Exception e) {
                e.printStackTrace();
                logger.error("ClientRecvDataBuffer processData, parse fail: ", e);
                iLastBuffer = 0;
                continue;
            }

            IMessage msg = result.oMessage;

            //循环加入消息处理器
            for (int i = 0; msg != null && i < result.iMessageNumber; i++) {
                if (msg.getInterMsgType() < ConstsMessage.MSG_TYPE_DATA_PUB) {
                    oMsgProcessor.addInMessage(msg);
                }
                else {
                    appendHandler.addMessage((Message) msg);
                }

                msg = msg.linkMessage();
            }
            buffer = null;
        }

        vecData.clear();
    }
}

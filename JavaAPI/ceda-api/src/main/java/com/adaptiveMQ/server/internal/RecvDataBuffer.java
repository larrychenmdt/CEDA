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

import com.adaptiveMQ.client.internal.MessageAppendHandler;
import com.adaptiveMQ.message.IMessage;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.internal.MessageConverter;
import com.adaptiveMQ.server.IServerLoginHandler;
import com.adaptiveMQ.server.IServerConnectionListener;
import com.adaptiveMQ.server.IServerConnectionMessageListener;
import com.adaptiveMQ.server.IServerSubscriptionHandler;
import com.adaptiveMQ.utils.BufferByte;
import com.adaptiveMQ.utils.Consts;
import com.adaptiveMQ.utils.ConstsMessage;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

public final class RecvDataBuffer extends Thread
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RecvDataBuffer.class);
    private final ConnectionPoint handler;
    private final BufferByte msgBuf;
    private final BufferByte bodyBuf;
    private final byte[] lastBuffer = new byte[Consts.MAX_RECEIVE_LEN]; //保存上次接收的不完整的数据
    //private Object obj = new Object();
    private final byte[] buffer2X = new byte[Consts.MAX_RECEIVE_2X_LEN];
    private ConnectionPointerProcessor oMsgProcessor = null;
    //private ConcurrentLinkedQueue<byte[]> m_vecData = null;
    private LinkedBlockingQueue<byte[]> vecData = null;
    //private Vector<byte[]> m_vecData = null;
    private boolean bRun = true;
    private MessageAppendHandler appendHandler = null;

    //private byte[] m_bdata= new byte[Consts.MAX_RECEIVE_LEN];
    private int iLastBuffer; //保存上次接收的不完整的数据长度

    public RecvDataBuffer(ConnectionPoint handler, IServerConnectionListener listener, IServerLoginHandler serverConnectionAuth)
    {
        this.handler = handler;
        msgBuf = new BufferByte();
        bodyBuf = new BufferByte();
        oMsgProcessor = new ConnectionPointerProcessor(this.handler, this.handler.getHandlerManage());
        oMsgProcessor.setConnectionListener(listener);
        oMsgProcessor.setLoginHandler(serverConnectionAuth);
        appendHandler = new MessageAppendHandler(oMsgProcessor);
        //m_vecData = new ConcurrentLinkedQueue<byte[]>();
        vecData = new LinkedBlockingQueue<byte[]>();
        this.start();
    }

    void setMessageHandler(IServerConnectionMessageListener msgListener)
    {
        oMsgProcessor.setMessageHandler(msgListener);
    }

    void setSubscriptionHandler(IServerSubscriptionHandler subscriptionHandler)
    {
        oMsgProcessor.setSubscriptionHandler(subscriptionHandler);
    }

    ConnectionPointerProcessor getProcess()
    {
        return oMsgProcessor;
    }

    void addData(byte[] btData)
    {
        vecData.add(btData);
    }

    void close()
    {
        bRun = false;
        this.interrupt();

        oMsgProcessor.close();
        appendHandler.close();
    }

    private void dealData(byte[] buffer, int iRecSize)
    {
        //将上次的剩余数据加入
        MessageConverter.StrByte2MsgOutput result = null;
        try {
            int ndeal = iLastBuffer + iRecSize;
            if (ndeal < 3) {
                //拷贝消息到lastbuffer
                System.arraycopy(buffer, 0, lastBuffer, iLastBuffer, iRecSize);
                iLastBuffer = ndeal;
                return;
            }

            if (iLastBuffer > 0) {
                System.arraycopy(lastBuffer, 0, buffer2X, 0, iLastBuffer);
            }

            //加入本次数据
            System.arraycopy(buffer, 0, buffer2X, iLastBuffer, iRecSize);

            //计算合法的数据
            //int iVaildSize = MessageConverter.getVaildByteSize(m_buffer2X,ndeal);
            result = MessageConverter.byte2VaildMsg(msgBuf, buffer2X, ndeal);
            int iVaildSize = result.iVaildByte;

            //将剩余数据保存
            iLastBuffer = iLastBuffer + iRecSize - iVaildSize;

            if (iLastBuffer > 0) {  //复制剩余数据
                System.arraycopy(buffer2X, iVaildSize, lastBuffer, 0, iLastBuffer);
            }
            else {
                //System.out.println("debug");
            }

            if (iVaildSize < 1) {
                return;
            }

        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error("RecvDataBuffer processData, parse fail: ", e);
            iLastBuffer = 0;
            return;
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
            //m_oMsgProcessor.addInMessage(msg);
            msg = msg.linkMessage();
        }
    }

    public void run()
    {
        while (bRun) {
            byte[] btData = null;
            try {
                btData = vecData.take();
            }
            catch (Exception e) {
                logger.error("take bytes[] failed", e);
            }

            if (btData == null) {
                continue;
            }
            if (!bRun) {
                break;
            }
            dealData(btData, btData.length);
        }

        msgBuf.close();
        bodyBuf.close();

    }
}

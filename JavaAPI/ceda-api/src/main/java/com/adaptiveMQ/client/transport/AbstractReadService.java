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

import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.client.internal.MessageProcessor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public abstract class AbstractReadService implements IClientReadService
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractReadService.class);
    private final String inFormat = "AbstractReadService receive bytes[%d]";

    protected MessageProcessor messageProcessor = null;
    private ClientRecvDataBuffer recvDataBuffer = null;        //缓冲接收到的数据

    //从客户端读取数据
    //private int m_iRecSize = 0;
    //private int m_iVaildSize = 0;
    //private byte[] m_buffer=null;
    //private byte[] m_buffer2X=null;

    public AbstractReadService(MessageProcessor msgProcessor)
    {
        messageProcessor = msgProcessor;
        //_iLastBuffer = 0;
    }

    public void work()
    {
        //启动数据缓冲
        recvDataBuffer = new ClientRecvDataBuffer(messageProcessor);
        recvDataBuffer.start();
        try {
            initRead();
        }
        catch (Exception e) {
            logger.error(e);
            messageProcessor.sendEvent(IEventListener.CONNECTION_CLOSED);
        }
    }

    public abstract void initRead() throws Exception;

    public void inputData(byte[] buffer, int iRecSize)
    {
        //int iVaildSize = 0;
        //CLog.debug(m_in_format,iRecSize);
        recvDataBuffer.addData(buffer);
    }

    public void close()
    {
        if (recvDataBuffer != null) {
            recvDataBuffer.close();
            recvDataBuffer = null;
        }
        destory();
    }

    public abstract void destory();
    //abstract public CParseResult parseData(byte[] inWork, int nlen);
}

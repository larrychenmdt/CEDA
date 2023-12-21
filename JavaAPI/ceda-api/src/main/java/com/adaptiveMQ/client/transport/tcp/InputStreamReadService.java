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

import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.client.internal.MessageProcessor;
import com.adaptiveMQ.client.transport.AbstractReadService;
import com.adaptiveMQ.utils.Consts;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamReadService extends AbstractReadService implements Runnable
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(InputStreamReadService.class);

    private Thread thdWorker = null;

    private InputStream oInput = null;
    private BufferedInputStream buffInput = null;

    public InputStreamReadService(MessageProcessor msgProcessor)
    {
        super(msgProcessor);
    }

    public void initRead() throws Exception
    {
        thdWorker = new Thread(this);
        thdWorker.start();
    }

    public void setStream(InputStream instream)
    {
        oInput = instream;
    }

    public void destory()
    {
        thdWorker.interrupt();
    }

    public void run()
    {
        try {
            //从客户端读取数据
            int iRecSize = 0;
            int iVaildSize = 0;
            byte[] buffer = new byte[Consts.MAX_RECEIVE_LEN];

            buffInput = new BufferedInputStream(oInput, Consts.MAX_RECEIVE_LEN);

            while (!thdWorker.isInterrupted()) {
                //iRecSize = m_oInput.read( buffer,0, Consts.MAX_RECEIVE_LEN );
                iRecSize = buffInput.read(buffer);
                if (iRecSize == -1) {
                    oInput.close();
                    break;
                }
                byte[] bout = new byte[iRecSize];
                System.arraycopy(buffer, 0, bout, 0, iRecSize);
                inputData(bout, iRecSize);
            }
        }
        catch (Exception e) {
            logger.error(e);
            messageProcessor.sendEvent(IEventListener.CONNECTION_CLOSED);
        }
        finally {
            if (buffInput != null) {
                try {
                    buffInput.close();
                }
                catch (Exception e) {
                    logger.error(e);
                }
            }
            if (oInput != null) {
                try {
                    oInput.close();
                }
                catch (IOException e) {
                    logger.error(e);
                }
            }
        }
    }
}

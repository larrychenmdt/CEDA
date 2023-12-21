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
import com.adaptiveMQ.message.ControlMessage;
import com.adaptiveMQ.message.IMessage;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.internal.MessageConverter;
import com.adaptiveMQ.utils.BufferByte;
import com.adaptiveMQ.utils.ConstsMessage;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

public abstract class AbstractWriteService extends Thread implements IClientWriteService
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractWriteService.class);
    //private Vector<IMessage> m_oQueue = null;//作为消息队列使用
    private LinkedBlockingQueue<IMessage> oQueue = null;    //作为消息队列使用
    private MessageProcessor oMsgProcessor = null;
    private int iHeartbeatInterval = 15000;    //使用心跳来向服务器说明客户端在线，默认不使用
    private int iPingInterval = 15000;            //默认用Ping用来监测逻辑连接是否正常，同时通知服务器客户端在线；
    private int iPingCounter = 0;                //用来记录连续发送Ping检测的次数
    private CheckThread checkThread = null;
    private boolean willCheck = true;

    //private Object obj = new Object();

    private boolean run = true;

    private BufferByte msgBuf;
    private BufferByte bodyBuf;

    public AbstractWriteService(MessageProcessor msgProcessor)
    {
        oQueue = new LinkedBlockingQueue<IMessage>();
        //m_oOutput = oOutput;
        oMsgProcessor = msgProcessor;
        msgBuf = new BufferByte();
        bodyBuf = new BufferByte();

    }

    public void setPingInterval(int interval)
    {
        iPingInterval = interval;
        iPingInterval = iPingInterval * 1000;    //将秒折算为毫秒
    }

    public void setHeartbeatInterval(int interval)
    {
        iHeartbeatInterval = interval;
        iHeartbeatInterval = iHeartbeatInterval * 1000;    //将秒折算为毫秒
    }

    //重新开始Ping
    public void resetPing()
    {
        iPingCounter = 0;            //次数重置
        willCheck = false;

    }

    public void run()
    {
        while (run) {
            //IMessage msg = null;
            //byte[] btOutput = null;

            //从消息对列里面取得消息

            IMessage msg = null;

            try {
                msg = oQueue.take();
            }
            catch (Exception e) {
                logger.error(e);
            }

            if (msg == null) {
                continue;
            }

            if (!run) {
                break;
            }

            try {
                Message[] msgList = MessageConverter.getGroupMessage(bodyBuf, msg);
                if (msgList == null) {
                    outMsg(msg);
                }
                else {
                    for (Message dmsg : msgList) {
                        outMsg(dmsg);
                    }
                    msgList = null;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.error(e);
                oMsgProcessor.sendEvent(IEventListener.CONNECTION_CLOSED);
            }
            msg = null;

        }

        //clear
        oQueue.clear();
        oQueue = null;

        msgBuf.close();
        bodyBuf.close();

        msgBuf = null;
        bodyBuf = null;
        //System.out.println("AbstractWriteService run stop");
    }

    //增加一个新的消息
    public void addMessage(IMessage msg)
    {
        oQueue.add(msg);
    }

    private void outMsg(IMessage msg) throws Exception
    {
        byte[] btOutput = msg.getStream();
        //int ilen=0;

        //msg.getStream(btOutput, ilen);
        if (btOutput == null) {
            //将消息编码
            btOutput = MessageConverter.msg2Byte(msgBuf, bodyBuf, msg);
        }

        //Ping的重值由Reader来决定
        //如果已经发送了消息，则准备发送下一个Ping

        if (btOutput != null) {
            output(btOutput, oQueue.size());
            //CLog.debug(m_out_format,btOutput.length);

        }

        msg = null;
        btOutput = null;
    }

    public abstract void output(byte[] btOutput, int size) throws Exception;

    public abstract void destory();

    public abstract void initWrite();

    public void close()
    {
        run = false;
        if (checkThread != null) {
            checkThread.stop();
            //m_checkThread = null;
        }

        this.interrupt();
        destory();
    }

    public void work()
    {
        this.start();
        initWrite();
        checkThread = new CheckThread();
    }

    private final class CheckThread implements Runnable
    {
        private Thread thdWorker = null;
        private int interval = iPingInterval;
        private boolean isPing = true;

        CheckThread()
        {
            thdWorker = new Thread(this);
            if (iPingInterval == 0) {
                interval = iHeartbeatInterval;
                isPing = false;
            }
            if (interval > 0) {
                thdWorker.start();
            }
        }

        void stop()
        {
            if (thdWorker != null) {
                thdWorker.interrupt();
                thdWorker = null;
            }
        }

        public void run()
        {
            //System.out.println("*** run in Check ***");
            willCheck = true;
            while (run) {
                try {
                    sleep(interval);
                }
                catch (Exception e) {
                }
                if (!run) {
                    break;
                }
                if (willCheck) {
                    if (iPingCounter > 1) {  //连续发送2次Ping都没有响应则认为逻辑上已经断开了连接
                        //CLog.printDebug(CLog.LEVEL_HIGH,"OutputStreamWriteService PingObject: Close a Connection");
                        oMsgProcessor.sendEvent(IEventListener.CONNECTION_CLOSED);
                        break;
                    }
                    else {
                        ControlMessage ctlMsg = new ControlMessage();
                        ctlMsg.setInterMsgType(ConstsMessage.MSG_TYPE_CTRL_HB);
                        //ctlMsg.setCommand("");
                        addMessage(ctlMsg);
                        iPingCounter++;    //计数器加1
                        logger.debug("OutputStreamWriteService: Sent a ping message");

                    }
                }
                else {
                    willCheck = true;
                }
            }
        }
    }
}

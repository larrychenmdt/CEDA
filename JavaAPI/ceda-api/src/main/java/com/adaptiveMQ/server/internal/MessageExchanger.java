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

import com.adaptiveMQ.message.BaseDestination;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.internal.MessageConverter;
import com.adaptiveMQ.server.IServerConnection;
import com.adaptiveMQ.utils.BufferByte;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public final class MessageExchanger extends Thread
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageExchanger.class);

    private final byte typeSub = 1;
    private final byte typeUnsub = 2;
    private final byte typeData = 0;
    private final BufferByte msgBuf;
    private final BufferByte bodyBuf;
    private final HashSet<ConnectionPoint> setConnection;
    private final String pubFormat = "publish msg, topic[%s], count[%d]";
    private final String subFormat = "Client[%d] subscribe topic[%s], count[%d]";
    private final String unsubFormat = "Client[%d] unSubscribe topic[%s], count[%d]";
    private CMessageCacheHandler cacheHandler;
    private LinkedBlockingQueue<CValue> msgQueue;
    private CDestinationManager destinationManager;
    private int iMessageIdSeed = 0;
    private boolean run = true;

    public MessageExchanger()
    {
        msgBuf = new BufferByte();
        bodyBuf = new BufferByte();
        msgQueue = new LinkedBlockingQueue<>();
        cacheHandler = new CMessageCacheHandler();
        destinationManager = new CDestinationManager();
        setConnection = new HashSet<>();
        this.start();
    }

    public void close()
    {
        run = false;
        this.interrupt();
    }

    public void subscribe(BaseDestination des, IServerConnection connHandler)
    {
        CValue cvalue = new CValue();
        cvalue.bType = typeSub;
        cvalue.obj1 = des.getName();
        cvalue.obj2 = connHandler;
        msgQueue.add(cvalue);
    }

    public void unSubscribe(BaseDestination des, IServerConnection connHandler)
    {
        CValue cvalue = new CValue();
        cvalue.bType = typeUnsub;
        cvalue.obj1 = des.getName();
        cvalue.obj2 = connHandler;
        msgQueue.add(cvalue);
    }

    private void addConsumer(String stopic, ConnectionPoint connHandler)
    {
        List<Message> msglist = cacheHandler.getMessage(stopic);
        for (Message msg : msglist) {
            connHandler.sendMessage(msg);
        }

        com.adaptiveMQ.server.internal.ConsumerList clist = destinationManager.getConsumerList(stopic);
        clist.addConnectionPoint(connHandler);
    }

    private void removeConsumer(String stopic, ConnectionPoint connHandler)
    {
        com.adaptiveMQ.server.internal.ConsumerList clist = destinationManager.getConsumerList(stopic);
        clist.removeConnectionPoint(connHandler);
    }

    public void addMessage(Message msg)
    {
        CValue cvalue = new CValue();
        cvalue.bType = typeData;
        cvalue.obj1 = msg;
        msgQueue.add(cvalue);
    }

    private void processMsg(Message msg)
    {
        try {
            msg.setMessageID(++iMessageIdSeed);
            byte[] bmsg = MessageConverter.msg2Byte(msgBuf, bodyBuf, msg);
            msg.setStream(bmsg);

            List<com.adaptiveMQ.server.internal.ConsumerList> conListList = destinationManager.getConsumerListSet(msg.getDestination().getName());
            for (com.adaptiveMQ.server.internal.ConsumerList conList : conListList) {
                Iterator<ConnectionPoint> it = conList.getList();
                while (it.hasNext()) {
                    ConnectionPoint conn = it.next();
                    if (!setConnection.contains(conn)) {
                        setConnection.add(conn);
                        conn.sendMessage(msg);
                    }
                }
            }

            setConnection.clear();
            cacheHandler.addMessage(msg);

        }
        catch (Exception e) {
            logger.error("failed to processMsg", e);
        }
    }

    public void run()
    {
        while (run) {
            CValue cvalue = null;
            try {
                cvalue = msgQueue.take();
            }
            catch (Exception e) {
                logger.error(e);
            }

            if (cvalue == null) {
                continue;
            }

            if (!run) {
                break;
            }

            if (cvalue.bType == typeData) {
                Message msg = (Message) cvalue.obj1;
                // logger.debug(String.format(pubFormat, msg.getDestination().getName(), msgQueue.size()));
                processMsg(msg);
            }
            else {
                String topic = (String) cvalue.obj1;
                ConnectionPoint connHandler = (ConnectionPoint) cvalue.obj2;
                if (cvalue.bType == typeSub) {
                    logger.debug(String.format(subFormat, connHandler.getConnectionID(), topic, msgQueue.size()));
                    addConsumer(topic, connHandler);
                }
                else if (cvalue.bType == typeUnsub) {
                    logger.debug(String.format(unsubFormat, connHandler.getConnectionID(), topic, msgQueue.size()));
                    removeConsumer(topic, connHandler);
                }
            }
        }
    }

    public boolean isRunning()
    {
        return run;
    }

    class CValue
    {
        byte bType;
        Object obj1;
        Object obj2;
    }
}

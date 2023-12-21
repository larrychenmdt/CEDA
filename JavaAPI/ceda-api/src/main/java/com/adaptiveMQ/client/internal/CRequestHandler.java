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

import com.adaptiveMQ.client.IMessageListener;
import com.adaptiveMQ.message.Message;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class CRequestHandler
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CRequestHandler.class);
    private MessageProcessor messageProcessor;
    private ConcurrentHashMap<String, RequetObject> reqHash;

    public CRequestHandler(MessageProcessor msgProcess)
    {
        messageProcessor = msgProcess;
        reqHash = new ConcurrentHashMap<>();
    }

    public void addRequest(Message msg, MessageRequestor req)
    {
        String sName = msg.getReplyTo().getName();
        logger.debug("CRequestHandler: add request message, topic=" + sName);
        if (reqHash.containsKey(sName)) {
            logger.error("have this request,topic=" + sName);
        }
        else {
            //m_reqHash.put(sName, req);
            RequetObject reqO = new RequetObject();
            reqO.reqObject = req;
            reqHash.put(sName, reqO);
        }
    }

    public void addRequest(Message msg, IMessageListener listener)
    {
        String sName = msg.getReplyTo().getName();
        logger.debug("CRequestHandler: add request message, topic=" + sName);
        RequetObject reqO = new RequetObject();
        reqO.reqType = 2;
        reqO.reqObject = listener;
        reqHash.put(sName, reqO);
    }

    public boolean onMessage(Message msg, String stopic)
    {
        logger.debug("CRequestHandler: add reply message, topic=" + stopic);

        RequetObject reqO = reqHash.remove(stopic);
        if (reqO == null) {
            return false;
        }
        else {
            if (reqO.reqType == 1) {
                MessageRequestor req = (MessageRequestor) reqO.reqObject;
                req.onMessage(msg);
                req.close();
            }
            else {
                IMessageListener listener = (IMessageListener) reqO.reqObject;
                listener.onMessage(msg);
            }
            return true;
        }
    }

    class RequetObject
    {
        byte reqType = 1;
        Object reqObject;
    }
}

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

import com.adaptiveMQ.client.ClientSession;
import com.adaptiveMQ.client.IMessageListener;
import com.adaptiveMQ.message.BaseDestination;
import com.adaptiveMQ.message.Destination;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.MessageBody;
import com.adaptiveMQ.message.MessageRecord;
import com.adaptiveMQ.message.internal.MsgHelper;
import com.adaptiveMQ.message.internal.CharsetConvertType;
import com.adaptiveMQ.utils.ConstsMessage;
import com.adaptiveMQ.utils.Utils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CSubscribeImageHandler
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(CSubscribeImageHandler.class);

    private final MessageProcessor messageProcessor;
    private final MessageProcessorIn messageProcessorIn;
    private final ClientSession clientSession;
    private final ConcurrentHashMap<String, CReqData> reqHash;
    private final Destination noPermissionDestination;

    public CSubscribeImageHandler(MessageProcessor oProcessor, ClientSession session)
    {
        reqHash = new ConcurrentHashMap<>();
        messageProcessor = oProcessor;
        messageProcessorIn = messageProcessor.getMessageProcessorIn();
        messageProcessorIn.setSubImageHander(this);
        clientSession = session;
        noPermissionDestination = new Destination(ConstsMessage.TOPIC_NO_PERMISSION);
    }

    public void addRequestTopic(List<BaseDestination> desList, String stopic, IMessageListener msgListener)
    {
        if (desList.size() < 1) {
            return;
        }
        CReqData reqData = new CReqData();
        for (BaseDestination des : desList) {
            reqData.desHash.put(des.getName(), des);
        }
        reqData.msgListener = msgListener;
        reqHash.put(stopic, reqData);
    }

    public void onMessage(Message msg, String stopic)
    {
        CReqData reqdata = reqHash.remove(stopic);
        if (reqdata == null) {
            logger.error("CSubscribeImageHandler onMessage(), can't get subscribe data, topic:" + stopic);
            return;
        }

        MessageBody body = msg.getMessageBody();
        try {
            if (body.haseField((short) 9) == MessageRecord.STRING) {
                String s9 = body.getString((short) 9);
                if (s9.length() > 3) {
                    Message npMsg = new Message();
                    npMsg.getMessageBody().addInt((short) 5, 0);
                    npMsg.setDestination(noPermissionDestination);
                    npMsg.getMessageBody().addString((short) 3, s9);
                    reqdata.msgListener.onMessage(npMsg);

                    logger.info("No permission: " + s9);
                    List<String> noList = MsgHelper.Deserialize(s9, List.class, CharsetConvertType.None);
                    for (String topic : noList) {
                        reqdata.desHash.remove(topic);
                    }
                }
            }

            //注册订阅
            clientSession.registerMessageListener(reqdata.desHash.values(), reqdata.msgListener);
            byte b5 = body.haseField((short) 5);
            int n5 = 0;
            if (b5 > 0) {
                n5 = body.getInt((short) 5);
            }
            else {
                return;
            }

            String svalue = null;

            //增加为blob时，获取内容
            switch (n5) {
                case 3: {
                    //blob 压缩
                    byte[] bdata = body.getBlobField();
                    svalue = Utils.ungzipByte(bdata);
                }
                break;
                case 2: {
                    //blob 不压缩
                    byte[] bdata = body.getBlobField();
                    svalue = Utils.getSysString(bdata);
                }
                break;
                case 1: {
                    //普通字段压缩
                    // byte[] bdata=body.getBlobField();
                    svalue = body.getGZIPString((short) 3);
                }
                break;

                default: {
                    //普通字段不压缩
                    svalue = body.getString((short) 3);
                }
                break;
            }

            Map<String, Object> hash = MsgHelper.Deserialize(svalue, Map.class, CharsetConvertType.None);
            String osrl = (String) hash.remove(ConstsMessage.SIGNAL_SRL);
            boolean bSrl = false;

            if (osrl == null || osrl.equals(ConstsMessage.STR_ONE)) {
                bSrl = true;
            }

            Set<Entry<String, Object>> set = hash.entrySet();
            for (Iterator<Entry<String, Object>> it = set.iterator(); it.hasNext(); ) {
                Entry<String, Object> ent = it.next();
                Message dmsg = new Message();
                dmsg.getDestination().setName(ent.getKey());
                dmsg.getMessageBody().addInt((short) 5, 0);
                Object ovalue = ent.getValue();
                String ss;
                if (bSrl) {
                    ss = MsgHelper.Serializer(ovalue, CharsetConvertType.None);
                }
                else {
                    ss = (String) ovalue;
                }

                dmsg.getMessageBody().addString((short) 3, ss);
                //设置
                dmsg.setUpdateType(Message.CACHE);
                reqdata.msgListener.onMessage(dmsg);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error("CSubscribeImageHandler onMessage() fail:", e);
        }
        reqHash.remove(stopic);
    }

    public class CReqData
    {
        public HashMap<String, BaseDestination> desHash = new HashMap<String, BaseDestination>();
        public IMessageListener msgListener;
    }
}

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

import com.adaptiveMQ.message.BaseDestination;
import com.adaptiveMQ.message.Message;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public final  class MessageCacheList
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageCacheList.class);

    private ConcurrentHashMap<String, Vector<Message>> oDestinationCacheList = null;

    public MessageCacheList()
    {
        oDestinationCacheList = new ConcurrentHashMap<String, Vector<Message>>();
    }

    private void updateCacheMessage(Message oSrcMsg, Message oDescMsg)
    {
        if (oSrcMsg == null || oDescMsg == null) {
            logger.error("MessageCacheList updateCacheMessage: Parameter is NULL");
            return;
        }
    }

    //添加一个发布消息
    public void addPublishMessage(Message msg)
    {
        if (msg == null) {
            logger.error("MessageCacheList addPublishMessage: Parameter is NULL");
            return;
        }
    }

    //获得一个地址里的消息列表
    public Vector<Message> getMessageList(BaseDestination desc)
    {
        Vector<Message> vecCacheList = null;
        //synchronized(_oDestinationCacheList)
        //{
        Vector<Message> oCacheList = null;
        oCacheList = oDestinationCacheList.get(desc.getName());
        if (oCacheList == null) { //没有找到
            return null;
        }
           //CLog.printDebug(CLog.LEVEL_HIGH, "MessageCacheList getMessageList: [" + desc.toString() + "] doesn't have CacheList");
        else {
            vecCacheList = oCacheList;
        }
        //}

        return vecCacheList;
    }

    //删除一个地址内的所有消息
    public void destoryMessageList(BaseDestination desc)
    {
        //synchronized(_oDestinationCacheList)
        //{
        Vector<Message> oCacheList = null;
        oCacheList = oDestinationCacheList.get(desc.getName());
        oDestinationCacheList.remove(desc.getName());
        oCacheList.removeAllElements();
        oCacheList = null;
        //}
    }
}

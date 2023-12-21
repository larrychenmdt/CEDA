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

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;

public class ConsumerList
{
    private CopyOnWriteArraySet<ConnectionPoint> connList = null;

    public ConsumerList()
    {
        connList = new CopyOnWriteArraySet<ConnectionPoint>();
    }

    //增加一个连接点
    public void addConnectionPoint(ConnectionPoint consumer)
    {
        connList.add(consumer);
    }

    //去掉一个连接点
    public void removeConnectionPoint(ConnectionPoint consumer)
    {
        connList.remove(consumer);
    }

    int getConsumerNumber()
    {
        return connList.size();
    }

    public Iterator<ConnectionPoint> getList()
    {
        return connList.iterator();
    }
}

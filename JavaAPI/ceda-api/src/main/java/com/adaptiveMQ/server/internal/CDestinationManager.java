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

import com.adaptiveMQ.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class CDestinationManager
{
    private final HashMap<String, ConsumerList> pHashGeneralDescList;
    private final HashMap<String, ConsumerList> pHashWildcardDescList;
    private final HashMap<String, ArrayList<ConsumerList>> hashDescCache;

    public CDestinationManager()
    {
        pHashGeneralDescList = new HashMap<String, ConsumerList>();
        pHashWildcardDescList = new HashMap<String, ConsumerList>();
        hashDescCache = new HashMap<String, ArrayList<ConsumerList>>();
    }

    public ConsumerList getConsumerList(String topic)
    {
        ConsumerList ret = null;
        if (isWildcard(topic)) {
            //
            ret = pHashWildcardDescList.get(topic);
            if (ret == null) {
                ret = new ConsumerList();
                pHashWildcardDescList.put(topic, ret);

                //Enumeration<String> em=m_HashDescCache.keys();
                Set<Entry<String, ArrayList<ConsumerList>>> set = hashDescCache.entrySet();
                Iterator<Entry<String, ArrayList<ConsumerList>>> it = set.iterator();
                while (it.hasNext()) {
                    Entry<String, ArrayList<ConsumerList>> entry = it.next();
                    if (Utils.matchDestination(topic, entry.getKey())) {
                        entry.getValue().add(ret);
                    }

                }
            }

        }
        else {
            ArrayList<ConsumerList> conList = hashDescCache.get(topic);
            if (conList == null) {
                conList = new ArrayList<ConsumerList>();
                hashDescCache.put(topic, conList);
            }

            ret = pHashGeneralDescList.get(topic);
            if (ret == null) {
                ret = new ConsumerList();
                pHashGeneralDescList.put(topic, ret);
                conList.add(ret);
            }

            Set<Entry<String, ConsumerList>> set = pHashWildcardDescList.entrySet();
            Iterator<Entry<String, ConsumerList>> it = set.iterator();

            while (it.hasNext()) {
                Entry<String, ConsumerList> entry = it.next();
                if (Utils.matchDestination(entry.getKey(), topic)) {
                    conList.add(entry.getValue());
                }
            }
        }

        return ret;
    }

    private boolean isWildcard(String topic)
    {
        return topic.indexOf("*") > -1;

    }

    public List<ConsumerList> getConsumerListSet(String topic)
    {
        ArrayList<ConsumerList> conListList = hashDescCache.get(topic);
        if (conListList == null) {
            conListList = new ArrayList<ConsumerList>();
            hashDescCache.put(topic, conListList);
            //check genal
            ConsumerList consumerGen = pHashGeneralDescList.get(topic);
            if (consumerGen != null) {
                conListList.add(consumerGen);
            }

            //check wild
            Set<Entry<String, ConsumerList>> set = pHashWildcardDescList.entrySet();
            Iterator<Entry<String, ConsumerList>> it = set.iterator();
            while (it.hasNext()) {
                Entry<String, ConsumerList> entry = it.next();
                if (Utils.matchDestination(entry.getKey(), topic)) {
                    conListList.add(entry.getValue());
                }

            }
        }
        return conListList;
    }
}

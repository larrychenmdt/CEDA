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

package com.adaptiveMQ.cluster.internal;

import com.adaptiveMQ.cluster.IRegisterClient;
import com.adaptiveMQ.cluster.IRegisterNodeListener;
import com.adaptiveMQ.cluster.RegisterHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegisterClient implements Watcher, IRegisterClient
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RegisterClient.class);
    private final ConcurrentHashMap<String, String> nodeHash;
    private final HashSet<String> oldList;
    private final HashSet<String> curList;
    private ZooKeeper zk = null;
    private String path;
    private boolean isWork = true;
    private IRegisterNodeListener registerListener = null;

    private RegisterHandler registerHandler = null;

    public RegisterClient(RegisterHandler registHandler)
    {
        nodeHash = new ConcurrentHashMap<String, String>();
        oldList = new HashSet<String>();
        curList = new HashSet<String>();

        registerHandler = registHandler;
        if (registerHandler.registerConneted()) {
            zk = registerHandler.getZooKeeper();
        }
    }

    public void onConnected()
    {
        zk = registerHandler.getZooKeeper();
    }

    //@Override
    public void setListener(IRegisterNodeListener listener, String path) throws Exception
    {
        if (!registerHandler.registerConneted()) {
            throw new Exception("must connected to zookeeper");
        }

        this.path = path;
        Stat stat2 = zk.exists(this.path, false);
        if (stat2 == null) {
            throw new Exception("can't find node: " + this.path);
            //m_zk.create(m_path, m_path.getBytes(), Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
        }

        registerListener = listener;
        initRegisterClient();
    }

    private void initRegisterClient() throws Exception
    {
        nodeHash.clear();
        try {
            byte[] bdata = zk.getData(path, this, null);
            nodeHash.put(path, new String(bdata));
        }
        catch (Exception e) {
            logger.error("initRegisterClient failed", e);
        }
        getChildren(null, null);
    }

    private void getData(Hashtable<String, String> addHash, Hashtable<String, String> changeHash, List<String> removeList)
    {
        try {
            if (addHash != null) {
                //add
                //change
                byte[] bdata = zk.getData(path, this, null);
                changeHash.put(path, new String(bdata));
            }
            else if (changeHash != null) {
                //change
                byte[] bdata = zk.getData(path, this, null);
                changeHash.put(path, new String(bdata));

            }
            else if (removeList != null) {
                //delete
                removeList.add(path);
            }
        }
        catch (Exception e) {
            logger.error("getData failed", e);
        }
    }

    void disConnect()
    {
        nodeHash.clear();
        isWork = false;
    }

    private void getChildren(Hashtable<String, String> addList, ArrayList<String> removeList)
    {
        //放在更改时重入
        oldList.clear();
        for (Enumeration<String> e = nodeHash.keys(); e.hasMoreElements(); ) {
            String skey = e.nextElement();
            if (!skey.equals(path)) {
                oldList.add(skey);
            }

        }

        //m_ServiceList.clear();
        curList.clear();
        try {
            List<String> lret = zk.getChildren(path, this);

            for (String spath : lret) {
                String sseqName = path + "/" + spath;
                byte[] bret = zk.getData(sseqName, false, null);
                String svalue = new String(bret);

                //m_ServiceList.add(sInfo);
                curList.add(sseqName);

                if (!oldList.contains(sseqName)) {
                    nodeHash.put(sseqName, svalue);
                    if (addList != null) {
                        addList.put(sseqName, svalue);
                    }
                }
            }

            if (removeList != null) {
                for (Iterator<String> it = oldList.iterator(); it.hasNext(); ) {
                    String sKey = it.next();
                    if (!curList.contains(sKey)) {
                        nodeHash.remove(sKey);
                        removeList.add(sKey);
                    }
                }
            }

        }
        catch (Exception e) {
            logger.error("getChildren failed", e);
        }
    }

    //client use
    public Map<String, String> getAllNode()
    {
        return nodeHash;
        //return retList;
    }

    public void process(WatchedEvent event)
    {
        if (!isWork) {
            return;
        }
        String eventPath = event.getPath();
        if (eventPath != null && (eventPath.indexOf(path) > -1)) {
            Hashtable<String, String> addHash = null;
            Hashtable<String, String> changeHash = null;
            ArrayList<String> removeList = null;
            if (event.getType() == EventType.NodeChildrenChanged) {
                addHash = new Hashtable<String, String>();
                removeList = new ArrayList<String>();
                getChildren(addHash, removeList);
            }
            else if (event.getType() == EventType.NodeDataChanged) {
                changeHash = new Hashtable<String, String>();
                getData(addHash, changeHash, removeList);
            }
            else if (event.getType() == EventType.NodeCreated) {
                addHash = new Hashtable<String, String>();
                getData(addHash, changeHash, removeList);

            }
            else if (event.getType() == EventType.NodeDeleted) {
                removeList = new ArrayList<String>();
                getData(addHash, changeHash, removeList);
                if (eventPath.equals(path)) {
                    logger.info("the RegisterClient can't listener node: " + path);
                }
            }
            if (registerListener != null) {
                registerListener.onNodeChange(addHash, changeHash, removeList);
            }
        }

    }

    public boolean exists(String path) throws Exception
    {
        Stat stat = zk.exists(path, false);
        return stat != null;
    }

    public String createNode(String spath, String svalue, boolean bTemp) throws Exception
    {
        if (zk == null) {
            throw new Exception("must connect zookeeper before");
        }
        Stat stat1 = zk.exists(spath, false);
        if (stat1 != null) {
            zk.setData(spath, svalue.getBytes(), -1);
            return spath;
        }

        String sret = "";
        if (bTemp) {
            sret = zk.create(spath, svalue.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        }
        else {
            sret = zk.create(spath, svalue.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        return sret;
    }

    public void deleteNode(String spath) throws Exception
    {
        if (zk == null) {
            throw new Exception("must connect zookeeper before");
        }

        Stat stat1 = zk.exists(spath, false);
        if (stat1 == null) {
            throw new Exception("can't find node path： " + spath);
        }
        zk.delete(spath, -1);
    }
}

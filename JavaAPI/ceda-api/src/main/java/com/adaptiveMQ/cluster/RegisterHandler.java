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

package com.adaptiveMQ.cluster;

import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.cluster.internal.RegisterClient;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RegisterHandler extends Thread implements Watcher
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RegisterClient.class);
    private static int statCode = 0;
    private final String root = "/MDTService";
    private String sHostPort = null;
    private ZooKeeper zk = null;
    private boolean registOK = false;
    private boolean isInit = false;
    private boolean bWillConnect = true;
    private ConcurrentHashMap<String, ClusterClient> clientHash = null;
    private ConcurrentLinkedQueue<IEventListener> eventList = null;
    private ConcurrentLinkedQueue<RegisterClient> registerClientList = null;

    public RegisterHandler()
    {
        clientHash = new ConcurrentHashMap<String, ClusterClient>();
        eventList = new ConcurrentLinkedQueue<IEventListener>();
        registerClientList = new ConcurrentLinkedQueue<RegisterClient>();
        this.start();
    }

    public void disConnectRegister()
    {
        close();
        bWillConnect = false;
    }

    private void close()
    {
        registOK = false;
        isInit = false;
        if (zk != null) {
            try {
                zk.close();
                zk = null;
            }
            catch (Exception e) {
                logger.error("close failed", e);
            }
        }
    }

    public synchronized void connectRegister(String sHostPort) throws Exception
    {
        if (!isInit) {
            bWillConnect = true;
            this.sHostPort = sHostPort;
            if (this.sHostPort == null) {
                throw new Exception("must setZookeeperAddress before");
            }
            try {
                //System.out.println("** new zookeeper");
                zk = new ZooKeeper(this.sHostPort, 90000, this);
                isInit = true;
            }
            catch (Exception e) {
                //e.printStackTrace();
                zk = null;
                throw e;
            }
        }
    }

    public void addEventListener(IEventListener regLister)
    {
        if (!eventList.contains(regLister)) {
            eventList.add(regLister);
        }
    }

    public void removeEventListener(IEventListener regLister)
    {
        eventList.remove(regLister);
    }

    public ZooKeeper getZooKeeper()
    {
        return zk;
    }

    String getPathRoot()
    {
        return root;
    }

    public synchronized ClusterClient getClusterClient(String sName) throws Exception
    {
        ClusterClient cclient = clientHash.get(sName);
        if (cclient == null) {
            cclient = new ClusterClient(root, sName, this);
            clientHash.put(sName, cclient);
        }
        return cclient;
    }

    public synchronized IRegisterClient getRegisterClient()
    {
        RegisterClient cclient = new RegisterClient(this);
        registerClientList.add(cclient);
        return cclient;
    }

    public boolean registerConneted()
    {
        return registOK;
    }

    private void onConnected()
    {
        Collection<ClusterClient> coll = clientHash.values();
        for (ClusterClient cClient : coll) {
            try {
                cClient.initClusterClient();
            }
            catch (Exception e) {
                logger.error("RegisterHandler onConnected(), error: ", e);
            }
        }

        for (RegisterClient regisClient : registerClientList) {
            regisClient.onConnected();
        }
    }

    public void process(WatchedEvent event)
    {
        boolean bDeal = false;
        int ncode = 0;
        KeeperState st = event.getState();
        if (st == KeeperState.SyncConnected) {
            ncode = IEventListener.CONNECTION_CONNECTED;
            if (statCode != ncode) {
                statCode = ncode;
                bDeal = true;
                if (!registOK) {
                    registOK = true;
                    onConnected();
                }

                logger.info("Event: Zookeeper connected success");
            }

        }
        else if (st == KeeperState.Disconnected) {
            ncode = IEventListener.CONNECTION_CLOSED;
            if (statCode != ncode) {
                logger.info("Evnet: disconnect from zookeeper");
                registOK = false;
                statCode = ncode;
                bDeal = true;
                logger.info("Event: Zookeeper CONNECTION_CLOSED");
            }

        }
        else if (st == KeeperState.Expired) {
            close();
            logger.info("Event: Zookeeper Expired");
        }

        if (bDeal) {
            for (IEventListener listener : eventList) {
                listener.onEvent(ncode);
            }
        }
    }

    @Override
    public void run()
    {
        while (true) {
            try {
                sleep(10000);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            if (bWillConnect) {
                if (!isInit && sHostPort != null) {
                    try {
                        this.connectRegister(sHostPort);
                    }
                    catch (Exception e) {
                        logger.error("connectRegister failed", e);
                    }
                }
            }
        }
    }
}

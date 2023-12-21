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

import com.adaptiveMQ.client.ProtocolType;
import com.adaptiveMQ.cluster.internal.ClusterComputBalance;
import com.adaptiveMQ.cluster.internal.ClusterComputRound;
import com.adaptiveMQ.cluster.internal.ClusterComputStandBy;
import com.adaptiveMQ.cluster.internal.IClusterComput;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClusterClient implements Watcher, IClusterClient
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClusterClient.class);
    private final String sName;
    private final String root;
    private final ConcurrentHashMap<String, ServiceInfo> serviceHash;
    private final ConcurrentHashMap<String, ServiceInfo> willServiceHash;
    private final HashSet<String> oldList;
    private final HashSet<String> curList;
    private final String serviceValue = null;
    private ZooKeeper zk = null;
    private String path;
    private String sType;
    private int nType = ServiceInfo.SERVICE_TYPE_ALL;
    private ArrayList<ServiceInfo> addList = null;
    private ArrayList<String> removeList = null;
    private IClusterComput clusterComput = null;

    private CopyOnWriteArrayList<IClusterListener> registerListener = null;

    private RegisterHandler registerHandler = null;

    private boolean isWork = true;

    //private boolean m_isWork=false;

    public ClusterClient(String sroot, String sName, RegisterHandler registHandler)
    {
        root = sroot;
        path = sroot + "/" + sName;
        this.sName = sName;

        serviceHash = new ConcurrentHashMap<String, ServiceInfo>();
        willServiceHash = new ConcurrentHashMap<String, ServiceInfo>();
        oldList = new HashSet<String>();
        curList = new HashSet<String>();

        registerListener = new CopyOnWriteArrayList<IClusterListener>();
        registerHandler = registHandler;
        if (registerHandler.registerConneted()) {
            zk = registerHandler.getZooKeeper();
            try {
                initClusterClient();
            }
            catch (Exception e) {
                logger.error(e);
            }

        }
    }

    //@Override
    public void removeListener(IClusterListener listener)
    {
        registerListener.remove(listener);
    }

    //@Override
    public void addListener(IClusterListener listener)
    {
        if (!registerListener.contains(listener)) {
            registerListener.add(listener);
        }

    }

    void disConnect()
    {
        serviceHash.clear();
        isWork = false;
    }

    void initClusterClient() throws Exception
    {
        isWork = true;
        zk = registerHandler.getZooKeeper();
        Stat stat2 = zk.exists(path, false);
        if (stat2 != null) {
            byte[] bvalue = zk.getData(path, false, null);
            sType = new String(bvalue);
            initClustComputer();
            getChildren(addList, removeList);
            for (IClusterListener regListener : registerListener) {
                regListener.onClustChange(addList, removeList);
            }
        }
        else {
            logger.error("no zookeeper node, path=" + path);
        }
    }

    public void addRegisterListener(IClusterListener registerListener)
    {
        if (!this.registerListener.contains(registerListener)) {
            this.registerListener.add(registerListener);
        }
    }

    private void initClustComputer() throws Exception
    {
        if (clusterComput == null) {
            if (sType.equals(ServiceInfo.SERVICE_TYPE[2])) {
                clusterComput = new ClusterComputStandBy();
                nType = ServiceInfo.SERVICE_TYPE_STANDBY;
            }
            else if (sType.equals(ServiceInfo.SERVICE_TYPE[3])) {
                clusterComput = new ClusterComputBalance();
                nType = ServiceInfo.SERVICE_TYPE_BALANCE;
            }
            else if (sType.equals(ServiceInfo.SERVICE_TYPE[4])) {
                clusterComput = new ClusterComputRound();
                nType = ServiceInfo.SERVICE_TYPE_ROUNDROBIN;
            }
        }
    }

    private ServiceInfo getServiceInfo(String seqName)
    {
        ServiceInfo sinfo = serviceHash.get(seqName);
        if (sinfo == null) {
            sinfo = new ServiceInfo();
            sinfo.setSequenceName(seqName);
        }
        return sinfo;

    }

    private void getChildren(ArrayList<ServiceInfo> addList, ArrayList<String> removeList)
    {
        oldList.clear();
        Collection<ServiceInfo> coll = serviceHash.values();
        for (ServiceInfo sInfor : coll) {
            oldList.add(sInfor.getSequenceName());
            //System.out.println("old:" + sInfor.getSequenceName());
        }

        //m_ServiceList.clear();
        curList.clear();
        willServiceHash.clear();
        try {
            List<String> lret = zk.getChildren(path, this);

            for (String spath : lret) {
                //System.out.println("in req: "+spath);
                String sseqName = path + "/" + spath;
                byte[] bret = zk.getData(sseqName, false, null);
                String svalue = new String(bret);

                ServiceInfo sInfo = parseServiceValue(sseqName, svalue);
                sInfo.setSequenceName(sseqName);
                //如果没有启动或者blance，需要监听
                if (!sInfo.getStat() || nType == ServiceInfo.SERVICE_TYPE_BALANCE || nType == ServiceInfo.SERVICE_TYPE_STANDBY) {
                    //System.out.println("regist path="+sseqName+", value"+svalue);

                    if (sInfo.getStat()) {
                        curList.add(sseqName);
                        if (!oldList.contains(sseqName)) {
                            serviceHash.put(sInfo.getSequenceName(), sInfo);
                            if (addList != null) {
                                // old not, be add

                                addList.add(sInfo);
                            }
                        }
                    }
                    else {
                        willServiceHash.put(sInfo.getSequenceName(), sInfo);
                    }

                    zk.exists(sseqName, this);
                }
                else {
                    //System.out.println("path="+sseqName+", value"+svalue);
                    //m_ServiceList.add(sInfo);
                    curList.add(sseqName);
                    if (!oldList.contains(sseqName)) {
                        serviceHash.put(sInfo.getSequenceName(), sInfo);
                        if (addList != null) {
                            // old not, be add

                            addList.add(sInfo);
                        }
                    }
                }

            }

            if (removeList != null) {
                for (Iterator<String> it = oldList.iterator(); it.hasNext(); ) {
                    String sKey = it.next();
                    if (!curList.contains(sKey)) {
                        serviceHash.remove(sKey);
                        removeList.add(sKey);
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error(e);
        }
    }

    //client use
    public List<ServiceInfo> getAllService()
    {
        ArrayList<ServiceInfo> retList = new ArrayList<ServiceInfo>();
        Collection<ServiceInfo> coll = serviceHash.values();
        for (ServiceInfo sinfo : coll) {
            retList.add(sinfo);
        }
        return retList;
        //return retList;
    }

    public List<ServiceInfo> getWillWorkService()
    {
        ArrayList<ServiceInfo> ret = new ArrayList<ServiceInfo>();
        Collection<ServiceInfo> coll = willServiceHash.values();
        for (ServiceInfo sinfo : coll) {
            ret.add(sinfo);
        }

        Collection<ServiceInfo> coll1 = serviceHash.values();
        for (ServiceInfo sinfo : coll1) {
            ret.add(sinfo);
        }

        if (clusterComput == null) {
            //System.out.println("client 2");
            return ret;
        }
        else {
            ServiceInfo sInfo = clusterComput.getComputService(ret);
            ArrayList<ServiceInfo> retList = new ArrayList<ServiceInfo>();

            //System.out.println("client 3");
            if (sInfo != null) {
                retList.add(sInfo);
            }
            //System.out.println("client 4");
            return retList;
        }
    }

    public List<ServiceInfo> getWorkService()
    {
        List<ServiceInfo> ret = getAllService();
        if (clusterComput == null) {
            //System.out.println("client 2");
            return ret;
        }
        else {
            ServiceInfo sInfo = clusterComput.getComputService(ret);
            ArrayList<ServiceInfo> retList = new ArrayList<ServiceInfo>();

            //System.out.println("client 3");
            if (sInfo != null) {
                retList.add(sInfo);
            }
            //System.out.println("client 4");
            return retList;
        }
    }

    private ServiceInfo parseServiceValue(String seqName, String svalue)
    {
        ServiceInfo sInfo = getServiceInfo(seqName);
        String[] sarray = svalue.split(",");

        for (int i = 0; i < sarray.length; i++) {
            String[] spair = sarray[i].split(":");
            if (spair.length == 2) {
                if (spair[0].equals("Host")) {
                    try {
                        sInfo.setHost(spair[1]);
                    }
                    catch (Exception e) {
                        logger.error(e);
                    }

                }
                else if (spair[0].equals("Port")) {
                    sInfo.setPort(Integer.parseInt(spair[1]));
                }
                else if (spair[0].equals("LBFactor")) {
                    sInfo.setLBFactor(Integer.parseInt(spair[1]));
                }
                else if (spair[0].equals("Stat")) {
                    sInfo.setStat(spair[1].equals("1"));

                }
                else if (spair[0].equals("Client")) {
                    sInfo.setClientNum(Integer.valueOf(spair[1]));
                }
                else if (spair[0].equals("Protocol")) {
                    sInfo.setProtocolType(ProtocolType.get(Integer.valueOf(spair[1])));
                }
            }
            else if (spair[0].equals("ShmPath")) {
                sInfo.setShareMemoryPath(sarray[i].substring(sarray[i].indexOf(":")+1));
            }

        }
        sInfo.setName(sName);
        return sInfo;
    }

    public void process(WatchedEvent event)
    {
        if (!isWork) {
            return;
        }
        if (event.getPath() != null && (event.getPath().indexOf(sName) > -1)) {
            if (addList == null) {
                addList = new ArrayList<ServiceInfo>();
                removeList = new ArrayList<String>();
            }

            addList.clear();
            removeList.clear();

            getChildren(addList, removeList);
            for (IClusterListener regListener : registerListener) {
                regListener.onClustChange(addList, removeList);
            }
        }
    }

    private void addServiceValue(StringBuffer strb, String sName, String svalue)
    {
        strb.append(sName);
        strb.append(svalue);
        strb.append(",");
    }

    private String getServiceValue(ServiceInfo serInfo)
    {
        StringBuffer strb = new StringBuffer();
        addServiceValue(strb, "Host:", serInfo.getHost());
        addServiceValue(strb, "Port:", String.valueOf(serInfo.getPort()));
        addServiceValue(strb, "LBFactor:", String.valueOf(serInfo.getLBFactor()));
        if (serInfo.getStat() || serInfo.getPort() == 0) {
            addServiceValue(strb, "Stat:", "1");
        }
        else {
            addServiceValue(strb, "Stat:", "0");
        }
        if (serInfo.getType() == ServiceInfo.SERVICE_TYPE_BALANCE) {
            addServiceValue(strb, "Client:", String.valueOf(serInfo.getClientNum()));
        }
        addServiceValue(strb, "ShmPath:", String.valueOf(serInfo.getShareMemoryPath()));
        addServiceValue(strb, "Protocol:", String.valueOf(serInfo.getProtocolType().value()));
        String svalue = strb.substring(0, strb.length() - 1);
        return svalue;
    }

    private void setSvrInfo(ServiceInfo serInfo, String spath) throws Exception
    {
        Stat statExist = null;
        if (serInfo.getSequenceName() != null) {
            statExist = zk.exists(serInfo.getSequenceName(), false);
        }
        if (statExist == null) {
            if (serInfo.getType() == ServiceInfo.SERVICE_TYPE_STANDBY) {
                if (serInfo.getStat()) {
                    List<ServiceInfo> svrList = getWorkService();
                    if (svrList != null && svrList.size() > 0) {
                        ServiceInfo svr = svrList.get(0);
                        //已经存在standBy
                        if (!svr.getSequenceName().equals(serInfo.getSequenceName())) {
                            serInfo.setStat(false);
                        }
                    }
                }
            }
            String svalue = getServiceValue(serInfo);
            String nodepath = spath;
            if (nodepath == null && serInfo.getSequenceName() != null) {
                //nodepath=spath;
                int npos = serInfo.getSequenceName().lastIndexOf('/');
                nodepath = serInfo.getSequenceName().substring(0, npos + 1) + serInfo.getName();
                //System.out.println("nodepath: "+nodepath);
            }
            String seqName = zk.create(nodepath, svalue.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            serInfo.setSequenceName(seqName);
        }
        else {
            String svalue = getServiceValue(serInfo);
            zk.setData(serInfo.getSequenceName(), svalue.getBytes(), -1);
        }
    }

    public synchronized void registService(ServiceInfo serInfo) throws Exception
    {
        if (!isWork) {
            logger.error("can't write to Zookeeper[" + serInfo.getName() + "] before connected");
            return;
        }
        if (zk == null) {
            throw new Exception("must init before");
        }

        Stat stat1 = zk.exists(root, false);
        if (stat1 == null) {
            zk.create(root, root.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }

        path = root + "/" + serInfo.getName();
        Stat stat2 = zk.exists(path, false);
        boolean bCreatePath = false;
        if (stat2 == null) {
            //m_sType=serInfo.getType();
            sType = ServiceInfo.SERVICE_TYPE[serInfo.getType()];
            zk.create(path, sType.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            bCreatePath = true;
        }
        else {
            byte[] bret = zk.getData(path, false, null);
            sType = new String(bret);
            if (!sType.equals(ServiceInfo.SERVICE_TYPE[serInfo.getType()])) {
                System.err.println("MQServer Type isn't equal Zookeeper Type");
            }
        }
        // init Clustcomputer
        initClustComputer();

        String sPath = path + "/" + serInfo.getName();

        setSvrInfo(serInfo, sPath);

        /****
         *  未找到创建节点的监听事件触发，此处自动发送一个事件
         *  目前程序BUG描述：当前新加入的节点不会出发process事件,只有当下一次再次注册时
         *  才会依赖已过期的节点出触发事件.
         */
        if (bCreatePath) {
            this.process(new WatchedEvent(EventType.NodeCreated, KeeperState.Unknown, sPath));
        }
    }

    public synchronized void removeServiceInfor(ServiceInfo serInfo) throws Exception
    {
        if (!isWork) {
            logger.error("can't write to Zookeeper[" + serInfo.getName() + "] before connected");
            return;
        }

        Stat statExist = null;
        if (serInfo.getSequenceName() != null) {
            statExist = zk.exists(serInfo.getSequenceName(), false);
        }

        if (statExist != null) {
            zk.delete(serInfo.getSequenceName(), -1);
        }

    }

    public void setServiceInfor(ServiceInfo serInfo) throws Exception
    {
        if (!isWork) {
            logger.error("can't write to Zookeeper[" + serInfo.getName() + "] before connected");
            return;
        }
        setSvrInfo(serInfo, null);
    }
}

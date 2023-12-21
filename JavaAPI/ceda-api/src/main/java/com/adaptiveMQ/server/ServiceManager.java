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

package com.adaptiveMQ.server;

import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.cluster.IClusterClient;
import com.adaptiveMQ.cluster.IRegisterClient;
import com.adaptiveMQ.cluster.RegisterHandler;
import com.adaptiveMQ.cluster.ServiceInfo;

public final class ServiceManager
{
    //private static ServiceManager m_oInstance=null;

    private RegisterHandler registHandler = null;

    //private  static Object initLock = new Object();

    private ServiceManager()
    {
        registHandler = new RegisterHandler();
    }

    /**
     * 获取ServiceManager实例
     *
     * @return ServiceManager
     * @throws
     */
    public static synchronized ServiceManager getInstance()
    {
        return InstanceContainer.instance;
    }

    /**
     * 连接RegisterServer
     *
     * @param String sHostPort，服务器间逗号分隔，类似"127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002"
     * @return void
     * @throws Exception
     */
    public synchronized void connectRegister(String sHostPort) throws Exception
    {
        registHandler.connectRegister(sHostPort);
    }

    /**
     * 断开RegisterServer
     *
     * @param null
     * @return void
     * @throws Exception
     */
    public void disConnectRegister()
    {
        registHandler.disConnectRegister();
    }

    /**
     * 返回register是否连接
     *
     * @param null
     * @return boolean: true,连接；false，没有连接
     * @throws
     */
    public boolean registerConnected()
    {
        return registHandler.registerConneted();
    }

    /**
     * 启动Service服务，socket服务器
     *
     * @param ServiceInfo               serviceInfo:服务地址端口信息
     * @param IServerConnectionListener handlerListen:服务端连接监听
     * @return IServerHandler
     * @throws
     */
    public IServerHandler startServer(ServiceInfo sInfo, IServerConnectionListener handlerListen, IServerLoginHandler loginHandler)
    {
        return MQServer.startMQ2App(sInfo, handlerListen, loginHandler, null);
    }

    /**
     * 启动Service服务，socket服务器，如果启动成功则注册zookeeper，否则不注册
     *
     * @param ServiceInfo               serviceInfo:服务地址端口信息
     * @param IServerConnectionListener handlerListen:服务端连接监听
     * @param IClusterClient            clustClient:集群客户端
     * @return IServerHandler
     * @throws
     */
    public IServerHandler startServerAndRegist(ServiceInfo sInfo, IServerConnectionListener handlerListen, IServerLoginHandler loginHandler, IClusterClient clustClient)
    {
        //CLog.printInfo("NettyServerTransport startMQ2App1");
        return MQServer.startMQ2App(sInfo, handlerListen, loginHandler, clustClient);
    }

    /**
     * 创建Clust客户端
     *
     * @param String sName 服务名称，类似OMS
     * @return IClusterClient
     * @throws Exception
     */
    public IClusterClient createClustClient(String sName) throws Exception
    {
        //m_registHandler.addRegisterListener(listener, sName);
        return registHandler.getClusterClient(sName);
    }

    /**
     * 创建Register客户端
     *
     * @param String sPaht 路径，类似ATFClient
     * @return void
     * @throws Exception
     */
    public IRegisterClient createRegisterClient() throws Exception
    {
        return registHandler.getRegisterClient();
    }

    /**
     * 增加Register连接监听事件
     *
     * @param IEventListener eventListener 监听器接口
     * @return void
     * @throws
     */
    public void addRegisterEventListener(IEventListener eventListener)
    {
        registHandler.addEventListener(eventListener);
    }

    /**
     * 移除Register连接监听事件
     *
     * @param IEventListener eventListener 监听器接口
     * @return void
     * @throws
     */
    public void removeRegisterEventListener(IEventListener eventListener)
    {
        registHandler.removeEventListener(eventListener);
    }

    private static class InstanceContainer
    {
        private static final ServiceManager instance = new ServiceManager();
    }
}

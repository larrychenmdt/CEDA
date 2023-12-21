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

import java.util.List;

public interface IClusterClient
{
    /**
     * 移除监听器
     *
     * @param IClusterListener listener，Register 监听器
     * @return void
     * @throws
     */
    void removeListener(IClusterListener listener);

    /**
     * 增加监听器
     *
     * @param IClusterListener listener，Register 监听器
     * @return void
     * @throws
     */
    void addListener(IClusterListener listener);
    /**
     * 注册服务，只有服务端才能调用
     *
     * @param ServiceInfo svrInfo，服务消息，包含host，port，name等
     * @return void
     * @throws Exception
     */
    void registService(ServiceInfo svrInfo) throws Exception;

    /**
     * 获取对应名称所有服务信息，Stat=1的服务
     *
     * @param String sName，服务名称，类似“webMQ”，可能返回超过一个服务
     * @return List，服务消息，包含host，port，name等
     * @throws
     */
    List<ServiceInfo> getAllService();

    /**
     * 获取计算的服务列表，当前应该提供服务的服务信息，Stat=1的服务,对应Standby,Loadbalance,All模式计算出的服务
     *
     * @param String sName，服务名称，类似“webMQ”，可能返回超过一个服务
     * @return List，服务消息，包含host，port，name等
     * @throws
     */
    List<ServiceInfo> getWorkService();

    /**
     * 获取对应名称，当前应该提供服务的服务信息，不考虑Stat，对应Standby,Loadbalance,All模式计算出的服务
     *
     * @param String sName，服务名称，类似“webMQ”，可能返回超过一个服务
     * @return List，服务消息，包含host，port，name等
     * @throws
     */
    List<ServiceInfo> getWillWorkService();

    /**
     * 修改已经注册服务，只有服务端才能调用
     *
     * @param ServiceInfo svrInfo，服务消息，包含客户端端数量等
     * @return void
     * @throws Exception
     */
    void setServiceInfor(ServiceInfo serInfo) throws Exception;

    void removeServiceInfor(ServiceInfo serInfo) throws Exception;
}

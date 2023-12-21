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

import java.util.Map;

public interface IRegisterClient
{
    /**
     * 增加监听器， 在连接上zookeeper后才能调用
     *
     * @param IRegisterNodeListener listener，目录监听器; String path 监听的目录
     * @return void
     * @throws Exception
     */
    void setListener(IRegisterNodeListener listener, String path) throws Exception;
    /**
     * 判断目录是否存在
     *
     * @param String path：对应zookeeper中的目录节点
     * @return boolean，存在true，不存在false；
     * @throws Exception
     */
    boolean exists(String path) throws Exception;

    /**
     * 获取当前目录下所有节点信息，
     *
     * @param null
     * @return Map，节点名称，节点内容
     * @throws
     */
    Map<String, String> getAllNode();
    /**
     * 创建节点，并保持值，或修改节点的内容
     *
     * @param String spath 路径,String svalue 字符串, boolean bTemp 是否临时的
     * @return String 返回创建后的节点名称
     * @throws
     */
    String createNode(String spath, String svalue, boolean bTemp) throws Exception;

    /**
     * 删除节点
     *
     * @param String spath 路径
     * @return void
     * @throws
     */
    void deleteNode(String spath) throws Exception;
}

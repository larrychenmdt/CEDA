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

package com.adaptiveMQ.client.transport;

import com.adaptiveMQ.message.IMessage;

public interface IClientWriteService
{
    void addMessage(IMessage msg);

    void close();                                //关闭写入线程

    void setPingInterval(int interval);            //设置Ping的间隔时间(单位为秒)

    void setHeartbeatInterval(int interval);    //设置心跳的间隔时间(单位为秒)

    void resetPing();                            //重新开始一次Ping的计时

    void work();
}

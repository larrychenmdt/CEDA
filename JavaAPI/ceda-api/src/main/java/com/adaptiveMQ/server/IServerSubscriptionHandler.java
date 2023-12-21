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

import com.adaptiveMQ.message.BaseDestination;

import java.util.List;

@FunctionalInterface
public interface IServerSubscriptionHandler
{

    /**
     * 响应客户端订阅消息
     *
     * @param List<BaseDestination> topicList，topic队列；IServerConnection connHandler 连接
     * @return void；
     * @throws
     */
    void onSubscribe(List<BaseDestination> topicList, IServerConnection connHandler, boolean isSubscribe);
}

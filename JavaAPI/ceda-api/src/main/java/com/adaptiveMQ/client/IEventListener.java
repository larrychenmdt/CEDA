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

package com.adaptiveMQ.client;

@FunctionalInterface
public interface IEventListener
{
    /**
     * 正在连接
     */
    int CONNECTION_CONNECTING = 0;

    /**
     * 连接上
     */
    int CONNECTION_CONNECTED = 1;

    /**
     * 连接关闭
     */
    int CONNECTION_CLOSED = 2;

    /**
     * 连接IO错误
     */
    int CONNECTION_IO_EXCEPTION = 3;

    /**
     * 重新连接
     */
    int CONNECTION_RECONNECT = 4;

    //public static final int CONNECTION_INVALID_PROTOCOL = 5;

    /**
     * 正在登录
     */
    int CONNECTION_LOGINING = 6;

    /**
     * 登录成功
     */
    int CONNECTION_LOGIN_SUCCESS = 7;

    /**
     * 连接丢失
     */
    int CONNECTION_LOST = 8;                    //连接掉线

    /**
     * 连接超时
     */
    int CONNECTION_TIMEOUT = 9;                    //超时

    /**
     * 连接错误
     */
    int ERR_CONNECTION_UNKNOW = 40;                //连接未知错误

    /**
     * 无密码
     */
    int ERR_CONNECTION_NULL_USER_PASSWD = 41;        //密码错误

    /**
     * 密码错误
     */
    int ERR_CONNECTION_USER_PASSWD = 42;        //密码错误

    /**
     * 客户ID已经存在
     */
    int ERR_CONNECTION_CLIENTID_EXIST = 43;        //ClientID exist

    /**
     * 未授权CA
     */
    int ERR_CONNECTION_AUTHORIZE_CA = 44;        //未授权 CA

    /**
     * 登陆模式错误
     */
    int ERR_CONNECTION_LOGIN = 46;

    /**
     * 订阅错误
     */
    int ERR_SUBSCRIB_UNKNOW = 60;                    //订阅未知错误;

    /**
     * 订阅成功
     */
    int SUCCESS_SUBSCRIB = 61;                    //订阅成功

    /**
     * 无订阅权限
     */
    int ERR_SUBSCRIB_PERIMISSION = 62;            //NO 订阅权限;

    /**
     * 订阅时未登录
     */
    int ERR_SUBSCRIB_LOGIN = 63;            //订阅时未登录;

    /**
     * 发送消息时，未知错误
     */
    int ERR_PUBLISH_UNKNOW = 70;            //发布未知错误;

    /**
     * 发送消息时,没有登录
     */
    int ERR_PUBLISH_LOGIN = 71;            //NO Login;

    /**
     * 发送消息时,没有发布权限
     */
    int ERR_PUBLISH_PERIMISSION = 72;        //NO 发布权限;

    /**
     * 响应事件
     *
     * @param int event：对应的事件代号
     * @return void；
     * @throws
     */
    void onEvent(int event);
}

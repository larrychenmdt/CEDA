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

package com.adaptiveMQ.utils;

public final class ConstsMessage
{
    private ConstsMessage() {};
    //控制消息
    //心跳消息
    public static final byte MSG_TYPE_CTRL_HB = 0x01;
    //订阅消息
    public static final byte MSG_TYPE_CTRL_SUB = 0x11;
    //退订消息
    public static final byte MSG_TYPE_CTRL_UNSUB = 0x12;
    //登录消息
    public static final byte MSG_TYPE_CTRL_LOGIN = 0x13;
    //登出消息
    public static final byte MSG_TYPE_CTRL_LOGOUT = 0x14;

    //普通数据消息
    public static final byte MSG_TYPE_DATA_PUB = 0x61;
    //请求消息
    public static final byte MSG_TYPE_DATA_REQ = 0x62;
    //回应消息
    public static final byte MSG_TYPE_DATA_REPLY = 0x63;
    //待定点对点消息
    public static final byte MSG_TYPE_DATA_P2P = 0x64;
    //普通大数据消息，需要分包
    public static final byte MSG_TYPE_DATA_PUB_GROUP = 0x65; // publish group 消息

    //控制号，配合控制消息使用
    //登录
    public static final byte CTRL_CODE_LOGIN = 3;
    //登录返回结果
    public static final byte CTRL_CODE_LOGIN_RESULT = 4;
    //登出
    public static final byte CTRL_CODE_LOGOUT = 5;
    //订阅，订阅结果
    public static final byte CTRL_CODE_SUB = 6;
    //订阅结果
    public static final byte CTRL_CODE_SUB_RESULT = 7;
    public static final byte CTRL_CODE_UNSUB = 8;
    public static final byte CTRL_CODE_UNSUB_RESULT = 9;
    public static final byte CTRL_CODE_SUB_BATCH = 13;
    public static final byte CTRL_CODE_UNSUB_BATCH = 14;
    public static final byte CTRL_CODE_LOGIN_SID = 18;
    public static final byte CTRL_CODE_LOGIN_VS = 19;
    public static final byte CTRL_CODE_SUBSCRIBE_WITH_IMAGE = 20;
    //end js

    public static final byte WEBSOCKET_BIN = (byte) -126;
    public static final byte BYTE126 = (byte) 126;
    public static final byte NEGATIVE_BYTE128 = (byte) -128;
    //确认消息的代码值
    public static final byte CODE_ACK_CLIENT_CONFIRMED = 1;        //消息被客户端确认
    public static final byte CODE_ACK_SERVER_CONFIRMED = 2;        //消息被服务器端确认
    public static final byte CODE_ACK_NO_PERMISSION = 3;        //没有权限

    public static final String TOPIC_SERVERINFO = "SYS.SERVICE.INFO";
    public static final String TOPIC_ATS_LOGIN = "SYS.ATS.LOGIN";
    public static final String TOPIC_API_LOGIN = "API.ATS.LOGIN"; //API登录topic
    public static final String SVR_VALIDATION = "ValidationServer";

    public static final String TOPIC_API_REQUESTIMAGE = "API.REQUESTIMAGE";
    public static final String TOPIC_ACS_REQUESTIMAGE = "ACS.REQUESTIMAGE";
    public static final String TOPIC_NO_PERMISSION = "NO.Permission";

    public static final String WEBSOCKET = "WebSocket";

    //反序列化标志，是否是值需要序列化
    public static final String SIGNAL_SRL = "SRL";

    public static final String STR_ONE = "1";
}

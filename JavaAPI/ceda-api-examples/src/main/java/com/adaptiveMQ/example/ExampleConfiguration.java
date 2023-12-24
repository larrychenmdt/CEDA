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

package com.adaptiveMQ.example;

import com.adaptiveMQ.client.ProtocolType;

public class ExampleConfiguration
{
     public static final ProtocolType protocolType = ProtocolType.PROTOCOL_TCP;
    //  public static final ProtocolType protocolType = ProtocolType.PROTOCOL_AERON_IPC;
    public static final String SERVER_HOST = "127.0.0.1";
    public static final int SERVER_PORT = 28086;
    public static final String CLIENT_HOST = "127.0.0.1";
    public static final int CLIENT_PORT = 28087;
    public static final String LOGIN_USER_NAME = "test";
    public static final String LOGIN_PASSWORD = "test";
    public static final String ZOOKEEPER_ADDRESS = "127.0.0.1:2181";
    public static final String SHM_BASE_PATH = "E:\\temp\\aeron_ipc\\";
    public static final String PONG_SERVER_NAME = "PongBlobServer";
    private ExampleConfiguration() {}
}

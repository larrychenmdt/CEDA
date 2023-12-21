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

import com.adaptiveMQ.client.ProtocolType;

public final class Consts
{
    private Consts() {};
    public static final String VERSION = "CEDA-api-5.0.1-SNAPSHOT Build: 20221208";

    public static final int MAX_DESTINATION_LENGTH = 255;
    public static final int MAX_MESSAGE_LENGTH = 65535;
    public static final int MAX_MESSAGE_HEAD_LENGTH = 535;
    public static final int MAX_MESSAGE_BODY_LENGTH = 65000;

    public static final int MAX_RECORD_FIELD = 4095;
    public static final int MAX_RECEIVE_LEN = 65535;
    public static final int MAX_RECEIVE_2X_LEN = 131070;

    public static final int MAX_USERNAME_LEN = 128;
    public static final int MAX_PASSWORD_LEN = 128;
    public static final int MAX_CLIENT_ID_LEN = 128;

    public static final String BlankStr = "";
    public static final  char SYSTEM_RESERVED_CHAR = '$';

    public static final int AERON_SERVER_DEFAULT_ACCEPT_STREAM_ID = 0;
    public static final int AERON_CLIENT_DEFAULT_STREAM_ID = 0;

    //public static final String AERON_CHANNEL_STRING_TEMPLATE = "aeron:udp?endpoint=%s:%d";
    //public static final String CHANNEL_STRING_TEMPLATE = "aeron:ipc";

    public static String getAeronChannelStringTemplate(ProtocolType protocolType)
    {
        switch (protocolType) {
            case PROTOCOL_AERON_UDP:
                return "aeron:udp?endpoint=%s:%d";
            case PROTOCOL_AERON_IPC:
                return "aeron:ipc";
            default:
                return "aeron:ipc";
        }
    }
}

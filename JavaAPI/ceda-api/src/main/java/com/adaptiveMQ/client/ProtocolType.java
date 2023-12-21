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

public enum ProtocolType
{
    /**
     * TCP 连接方式
     */
    PROTOCOL_TCP(1, "TCP"),

    /**
     * TCPS 连接方式，TCP SSL连接
     */
    PROTOCOL_TCPS(2, "TCPS"),

    /**
     * WS websocket连接方式，需要ACS支持
     */
    PROTOCOL_HTTP(3, "HTTP"),

    /**
     * HTTPS连接，需要ACS支持
     */
    PROTOCOL_HTTPS(4, "HTTPS"),

    /**
     * AERON连接方式
     */
    PROTOCOL_AERON_UDP(5, "AERON"),

    /**
     * AERON IPC连接方式
     */
    PROTOCOL_AERON_IPC(6, "AERON_IPC"),

    /**
     * To be used to represent not present or null.
     */
    NULL_VAL(-2147483648, "UNKNOW");

    private final int value;
    private final String description;

    ProtocolType(final int value, final String description)
    {
        this.value = value;
        this.description = description;
    }

    /**
     * Lookup the enum value representing the value.
     *
     * @param value encoded to be looked up.
     * @return the enum value representing the value.
     */
    public static ProtocolType get(final int value)
    {
        switch (value) {
            case 1:
                return PROTOCOL_TCP;
            case 2:
                return PROTOCOL_TCPS;
            case 3:
                return PROTOCOL_HTTP;
            case 4:
                return PROTOCOL_HTTPS;
            case 5:
                return PROTOCOL_AERON_UDP;
            case 6:
                return PROTOCOL_AERON_IPC;
            case -2147483648:
                return NULL_VAL;
        }

        throw new IllegalArgumentException("Unknown value: " + value);
    }

    /**
     * The raw encoded value in the Java type representation.
     *
     * @return the raw value encoded.
     */
    public int value()
    {
        return value;
    }

    /**
     * The raw encoded value in the Java type representation.
     *
     * @return the raw value encoded.
     */
    public String description()
    {
        return description;
    }
}

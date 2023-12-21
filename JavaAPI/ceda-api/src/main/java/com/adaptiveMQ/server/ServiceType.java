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

public enum ServiceType
{
    /**
     * 暂不使用
     */
    SERVICE_TYPE_ALL(1, "ALL"),

    /**
     * stand-by集群
     */
    SERVICE_TYPE_STANDBY(2, "STANDBY"),

    /**
     * hot-hot 集群
     */
    SERVICE_TYPE_BALANCE(3, "BALANCE"),

    /**
     * round-robin
     */
    SERVICE_TYPE_ROUNDROBIN(4, "ROUNDROBIN"),

    /**
     * To be used to represent not present or null.
     */
    NULL_VAL(-2147483648, "UNKNOW");

    private final int value;
    private final String description;

    ServiceType(final int value, final String description)
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
    public static ServiceType get(final int value)
    {
        switch (value) {
            case 1:
                return SERVICE_TYPE_ALL;
            case 2:
                return SERVICE_TYPE_STANDBY;
            case 3:
                return SERVICE_TYPE_BALANCE;
            case 4:
                return SERVICE_TYPE_ROUNDROBIN;
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

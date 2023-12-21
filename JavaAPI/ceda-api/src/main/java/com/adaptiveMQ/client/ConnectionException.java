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

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public final class ConnectionException extends Exception
{
    static final long serialVersionUID = 0x00ff17;
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnectionException.class);

    /**
     * 构造函数
     *
     * @param String strMessage ： 错误字符串
     * @return ConnectionException: 返回的类
     * @throws
     */
    public ConnectionException(String strMessage)
    {
        super(strMessage);
        logger.error("ConnectionException: " + strMessage);
    }
}

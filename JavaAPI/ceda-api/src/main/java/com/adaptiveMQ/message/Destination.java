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

package com.adaptiveMQ.message;

public class Destination extends BaseDestination
{
    /*
    final static public byte PUBLISH = 0;
    final static public byte QUEUE = 1;
    final static protected byte TEMP = 2;//客户端API不能直接用Temp地址
    final static protected byte ADMIN = 3;//客户端API不能直接用Admin地址
    final static public byte AMMB = 4;//AMMB消息类型
    */
    /**
     * 构造函数
     */
    /*
    public Destination()
    {
        super();
    }
    */

    /**
     * 构造函数
     *
     * @param String szName： 地址名称
     * @return Destination： 消息地址类
     * @throws
     */
    //*
    public Destination(String szName)
    {
        super(szName);
    }
    //*/

    //判断用户的地址是否合法
    /**
     *
     * 设置地址名称
     * @param String szName： 地址名称
     * @return void
     * @exception
     */
    /*
    public void setName(String szName)
    {
        if(szName.indexOf(Consts.SYSTEM_RESERVED_CHAR) != -1)
        {
            CLog.printError(33,CLog.LEVEL_LOW,"Destination cannot use " + Consts.SYSTEM_RESERVED_CHAR);
            return;
        }

        super.setName(szName);
    }
    */
}

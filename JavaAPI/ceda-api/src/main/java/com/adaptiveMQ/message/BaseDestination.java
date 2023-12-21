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

import com.adaptiveMQ.utils.Consts;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class BaseDestination
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(BaseDestination.class);

    /**
     * 发布类消息地址
     */
    //final static public byte PUBLISH = 0;

    /**
     * 队列类消息地址
     */
    //final static public byte QUEUE = 1;

    /**
     * 临时消息地址
     */
    //final static public byte TEMP = 2;

    /**
     * 管理类消息地址
     */
    //final static public byte ADMIN = 3;

    /**
     * AMMB类消息地址，没有使用
     */
    //@Deprecated
    //final static public byte AMMB = 4;//AMMB消息类型

    protected String szName = null;
    //protected String m_szToString;
    //protected byte m_nType;
    //protected boolean m_bIsPersistent;

    /**
     * 构造函数
     */
    public BaseDestination()
    {
        //m_bIsPersistent = false;
        //m_nType = PUBLISH;
    }

    /**
     * 构造函数
     *
     * @param String szName： 地址名称
     * @return BaseDestination： 消息地址类
     * @throws
     */
    public BaseDestination(String szName)
    {
        //m_bIsPersistent = false;
        //m_nType = PUBLISH;
        setName(szName);
    }

    //判断用户的地址是否合法

    /**
     * 设置地址名称
     *
     * @param null
     * @return String： 地址名称
     * @throws
     */
    public String getName()
    {
        return szName;
    }

    /**
     * 设置地址名称
     *
     * @param String szName： 地址名称
     * @return void
     * @throws
     */
    public void setName(String szName)
    {
        if (szName == null) {
            logger.error("Destination is NULL");
            return;
        }

        if (szName.length() >= Consts.MAX_DESTINATION_LENGTH) {
            logger.error("Destination(" + szName + ") has to be less than " + Consts.MAX_DESTINATION_LENGTH);
            return;
        }

        this.szName = szName;
        //makeString();
    }

    /*
    private void makeString()
    {
        if(m_nType == PUBLISH)
            m_szToString = "P#" + m_szName;
        else if(m_nType == QUEUE)
            m_szToString = "Q#" + m_szName;
        else if(m_nType == TEMP)
            m_szToString = "T#" + m_szName;
        else if(m_nType == ADMIN)
            m_szToString = "A#" + m_szName;
        else if(m_nType == AMMB)
            m_szToString = "B#" + m_szName;
        else
            m_szToString = "U#" + m_szName;
    }
    */

    /**
     * 获取地址字符串
     *
     * @param null
     * @return String： 地址名称
     * @throws
     */
    //*
    public String toString()
    {
        //return m_szToString;
        return szName;
    }
    //*/

    /**
     *
     *设置地址类型
     * @param byte type：地址类型
     * @return void
     * @exception
     */
    /*
    public void setType(byte type)
    {
        m_nType = type;
        if(m_szName != null)
        {
            makeString();
        }
    }
    */

    /**
     *
     *获取地址类型
     * @param null
     * @return byte type：地址类型
     * @exception
     */
    /*
    public byte getType()
    {
        return m_nType;
    }
    */

    //是否需要持久化
    /**
     *
     *设置持久化类型
     * @param boolean isPersistent： 持久化参数
     * @return void
     * @exception
     */
    /*
    public void isPersistent(boolean isPersistent)
    {
        m_bIsPersistent = isPersistent;
    }
    */

    /**
     *
     *获取持久化类型
     * @param null
     * @return boolean： 持久化参数
     * @exception
     */
    /*
    public boolean isPersistent()
    {
        return m_bIsPersistent;
    }
    */

    /**
     * 地址复制
     *
     * @param BaseDestination dest： 源地址
     * @return void;
     * @throws
     */
    public void copy(BaseDestination dest)
    {
        if (dest == null) {
            return;
        }
        //setType(dest.getType());
        setName(dest.getName());

        //isPersistent(dest.isPersistent());
    }
}

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
import com.adaptiveMQ.utils.Utils;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

public class MessageRecord
{
    /**
     * 无该字段
     */
    public static final byte NONE = 0;

    /**
     * 字段为int
     */
    public static final byte INT = 1;

    /**
     * 字段为long
     */
    public static final byte LONG = 2;

    /**
     * 字段为short
     */
    public static final byte SHORT = 3;

    /**
     * 字段为float
     */
    public static final byte FLOAT = 4;

    /**
     * 字段为double
     */
    public static final byte DOUBLE = 5;

    /**
     * 字段为String
     */
    public static final byte STRING = 6;

    /**
     * 字段为bytes
     */
    public static final byte BYTES = 7;
    private Hashtable<Short, CField> hashMsg = null;
    private MessageAppend msgAppend;

    /**
     * 构造函数
     */
    public MessageRecord()
    {
        hashMsg = new Hashtable<Short, CField>();
    }

    /**
     * 获取扩展类
     *
     * @param null
     * @return MessageAppend ：消息扩展类
     * @throws
     */
    public MessageAppend getAppend()
    {
        return msgAppend;
    }

    /**
     * 设置扩展类
     *
     * @param MessageAppend append：消息扩展类
     * @return void
     * @throws
     */
    public void setAppend(MessageAppend append)
    {
        msgAppend = append;
    }

    /**
     * 获取Bolb字段内容
     *
     * @param null
     * @return byte[] ：blob字段内容
     * @throws
     */
    public byte[] getBlobField()
    {
        if (msgAppend == null) {
            return null;
        }
        else {
            return msgAppend.getBlobData();
        }

    }

    /**
     * 设置blob字段，和占用的位置；
     *
     * @return void
     * @throws MessageBodyException
     * @paramshort nPosition, byte[] bdata:服务地址端口信息
     */
    public void setBlobField(short nPosition, byte[] bdata) throws MessageBodyException
    {
        if (msgAppend == null) {
            msgAppend = new MessageAppend();
        }
        try {
            msgAppend.setBlobField(nPosition, bdata);
        }
        catch (Exception e) {
            throw new MessageBodyException("setBlobField fail:" + e);
        }
    }

    /**
     * 增加一个String字段；
     *
     * @return void
     * @throws MessageBodyException
     * @paramshort short nPosition：字段位置, String strField:字段内容
     */
    public int addString(short nPosition, String strField) throws Exception
    {
        CField oField = new CField();

        oField.nFieldType = STRING;
        oField.nPosition = nPosition;
        //oField.iLen = (short)(strField.getBytes().length + 1);
        //oField.iLen = strField.getBytes().length;
        oField.iLen = Utils.getSysByte(strField).length;
        oField.oField = strField;

        addField(nPosition, oField);

        return nPosition;
    }

    /**
     * 增加一个压缩String字段；
     *
     * @return void
     * @throws MessageBodyException
     * @paramshort short nPosition：字段位置, String strField:字段内容
     */
    public int addGZIPString(short nPosition, String strField) throws MessageBodyException
    {
        CField oField = new CField();

        oField.nFieldType = BYTES;
        oField.nPosition = nPosition;
        //oField.iLen = (short)(strField.getBytes().length + 1);
        byte[] sbyte = Utils.gzipStr(strField);
        if (sbyte == null) {
            throw new MessageBodyException("can't GZIP the string");
        }
        oField.iLen = sbyte.length;
        oField.oField = sbyte;

        addField(nPosition, oField);

        return nPosition;
    }

    @Deprecated
    public int addZlibString(short nPosition, String strField) throws MessageBodyException
    {
        CField oField = new CField();

        oField.nFieldType = BYTES;
        oField.nPosition = nPosition;
        //oField.iLen = (short)(strField.getBytes().length + 1);
        byte[] sbyte = Utils.zlibStr(strField);
        if (sbyte == null) {
            throw new MessageBodyException("can't GZIP the string");
        }
        oField.iLen = sbyte.length;
        oField.oField = sbyte;

        addField(nPosition, oField);

        return nPosition;
    }

    /**
     * 增加一个int字段；
     *
     * @return void
     * @throws MessageBodyException
     * @paramshort short nPosition：字段位置, int iField:int值
     */
    public int addInt(short nPosition, int iField) throws MessageBodyException
    {
        CField oField = new CField();

        oField.nFieldType = INT;
        oField.nPosition = nPosition;
        oField.iLen = 4;
        oField.oField = new Integer(iField);

        addField(nPosition, oField);

        return nPosition;
    }

    /**
     * 增加一个long字段；
     *
     * @return void
     * @throws MessageBodyException
     * @paramshort short nPosition：字段位置, long lField:long值
     */
    public int addLong(short nPosition, long lField) throws MessageBodyException
    {
        CField oField = new CField();

        oField.nFieldType = LONG;
        oField.nPosition = nPosition;
        oField.iLen = 8;
        oField.oField = new Long(lField);

        addField(nPosition, oField);

        return nPosition;
    }

    /**
     * 增加一个short字段；
     *
     * @return void
     * @throws MessageBodyException
     * @paramshort short nPosition：字段位置, short sField:short值
     */
    public int addShort(short nPosition, short sField) throws MessageBodyException
    {
        CField oField = new CField();

        oField.nFieldType = SHORT;
        oField.nPosition = nPosition;
        oField.iLen = 2;
        oField.oField = new Short(sField);

        addField(nPosition, oField);

        return nPosition;
    }

    //增加一个short字段

    /**
     * 增加一个float字段；
     *
     * @return void
     * @throws MessageBodyException
     * @paramshort short nPosition：字段位置, float fField:float值
     */
    public int addFloat(short nPosition, float fField) throws MessageBodyException
    {
        CField oField = new CField();

        oField.nFieldType = FLOAT;
        oField.nPosition = nPosition;
        oField.oField = new Float(fField);
        oField.iLen = 4;

        addField(nPosition, oField);

        return nPosition;
    }

    //增加一个float字段

    /**
     * 增加一个double字段；
     *
     * @return void
     * @throws MessageBodyException
     * @paramshort short nPosition：字段位置, double dField:double值
     */
    public int addDouble(short nPosition, double dField) throws MessageBodyException
    {
        CField oField = new CField();

        oField.nFieldType = DOUBLE;
        oField.nPosition = nPosition;
        oField.iLen = 8;
        oField.oField = new Double(dField);

        addField(nPosition, oField);

        return nPosition;
    }

    //增加一个double字段

    /**
     * 返回指定位置的字段类型；
     *
     * @return byte：字段类型
     * @throws
     * @paramshort short nPosition：字段位置
     */
    public byte haseField(short nPosition)
    {
        CField oField = hashMsg.get(new Short(nPosition));
        if (oField == null) {
            return NONE;
        }
        else {
            return oField.nFieldType;
        }
    }

    /**
     * 增加一个bytes字段；
     *
     * @return void
     * @throws MessageBodyException
     * @paramshort short nPosition：字段位置, byte[] btField: byte[]内容
     */
    public int addBytes(short nPosition, byte[] btField) throws MessageBodyException
    {
        CField oField = new CField();

        oField.nFieldType = BYTES;
        oField.nPosition = nPosition;
        //oField.iLen = (short)btField.length;
        oField.iLen = btField.length;
        oField.oField = btField;

        addField(nPosition, oField);

        return nPosition;
    }

    /**
     * 返回字段数目；
     *
     * @return int
     * @throws
     * @paramshort null
     */
    public int size()
    {
        return hashMsg.size();
    }

    /**
     * 获取字段字符串内容；
     *
     * @return String： 字符串内容
     * @throws MessageBodyException
     * @paramshort short nPosition
     */
    public String getString(short nPosition) throws MessageBodyException
    {
        CField oField = null;

        oField = getField(nPosition, STRING);
        return (String) oField.oField;
    }

    /**
     * 获取压缩字段字符串内容；
     *
     * @return String： 字符串内容
     * @throws MessageBodyException
     * @paramshort short nPosition
     */
    public String getGZIPString(short nPosition) throws MessageBodyException
    {
        CField oField = null;

        oField = getField(nPosition, BYTES);
        String sout = Utils.ungzipByte((byte[]) oField.oField);
        return sout;
    }

    @Deprecated
    public String getZlibString(short nPosition) throws MessageBodyException
    {
        CField oField = null;

        oField = getField(nPosition, BYTES);
        String sout = Utils.unzlibByte((byte[]) oField.oField);
        return sout;
    }

    /**
     * 获取字段int值；
     *
     * @return int： 值
     * @throws MessageBodyException
     * @paramshort short nPosition
     */
    public int getInt(short nPosition) throws MessageBodyException
    {
        CField oField = null;

        oField = getField(nPosition, INT);

        return ((Integer) oField.oField).intValue();
    }

    /**
     * 获取字段long值；
     *
     * @return long： 值
     * @throws MessageBodyException
     * @paramshort short nPosition
     */
    public long getLong(short nPosition) throws MessageBodyException
    {
        CField oField = null;

        oField = getField(nPosition, LONG);

        return ((Long) oField.oField).longValue();
    }

    /**
     * 获取字段short值；
     *
     * @return short： 值
     * @throws MessageBodyException
     * @paramshort short nPosition
     */
    public short getShort(short nPosition) throws MessageBodyException
    {
        CField oField = null;

        oField = getField(nPosition, SHORT);

        return ((Short) oField.oField).shortValue();
    }

    /**
     * 获取字段float值；
     *
     * @return float： 值
     * @throws MessageBodyException
     * @paramshort short nPosition
     */
    public float getFloat(short nPosition) throws MessageBodyException
    {
        CField oField = null;

        oField = getField(nPosition, FLOAT);

        return ((Float) oField.oField).floatValue();
    }

    /**
     * 获取字段double值；
     *
     * @return double： 值
     * @throws MessageBodyException
     * @paramshort short nPosition
     */
    public double getDouble(short nPosition) throws MessageBodyException
    {
        CField oField = null;

        oField = getField(nPosition, DOUBLE);

        return ((Double) oField.oField).doubleValue();
    }

    /**
     * 获取字段byte[]内容；
     *
     * @return byte[]：内容
     * @throws MessageBodyException
     * @paramshort short nPosition
     */
    public byte[] getBytes(short nPosition) throws MessageBodyException
    {
        CField oField = null;

        oField = getField(nPosition, BYTES);

        return (byte[]) oField.oField;
    }

    /**
     * 清除所有字段；
     *
     * @return void
     * @throws
     * @paramshort null
     */
    public void clear()
    {
        if (hashMsg != null) {
            hashMsg.clear();
        }
    }

    /**
     * 清除指定位置字段；
     *
     * @return void
     * @throws
     * @paramshort short nPosition:对应位置
     */
    public void removeField(short nPosition)
    {
        hashMsg.remove(new Short(nPosition));
    }

    /**
     * 返回字段数目；
     *
     * @return short
     * @throws
     * @paramshort null
     */
    public short getSize()
    {
        return (short) hashMsg.size();
    }

    /**
     * 遍历字段；用getFieldIterator替换
     *
     * @return Enumeration<CField>
     * @throws
     * @paramshort null
     */
    @Deprecated
    public Enumeration<CField> getValues()
    {
        return hashMsg.elements();
    }

    /**
     * 遍历字段；
     *
     * @return Enumeration<CField>
     * @throws
     * @paramshort null
     */
    public Iterator<CField> getFieldIterator()
    {
        return hashMsg.values().iterator();
    }

    /**
     * 增加字段；
     *
     * @return void
     * @throws MessageBodyException
     * @paramshort short nPosition:对应位置, CField oField：字段类
     */
    public void addField(short nPosition, CField oField) throws MessageBodyException
    {
        //检查FID不能超过标准
        if (nPosition >= Consts.MAX_RECORD_FIELD) {
            throw new MessageBodyException("Field ID(" + nPosition + ") can't be more than " + Consts.MAX_RECORD_FIELD);
        }

        hashMsg.put(new Short(nPosition), oField);
        /*
        synchronized(m_hashMsg)
        {
        }
        */
    }

    private CField getField(short nPosition, byte nFieldType) throws MessageBodyException
    {
        //检查FID不能超过标准
        if (nPosition >= Consts.MAX_RECORD_FIELD) {
            throw new MessageBodyException("Field ID(" + nPosition + ") can't be more than " + Consts.MAX_RECORD_FIELD);
        }

        CField oField = null;

        oField = hashMsg.get(new Short(nPosition));

        if (oField == null) {
            throw new MessageBodyException("Field(" + nPosition + ") is NULL");
        }

        //判断所需要的数据类型是否与保存类型一致
        if (oField.nFieldType != nFieldType) {
            throw new MessageBodyException("Field Type is incompatible, Position=" + nPosition);
        }

        return oField;
    }

    public class CField
    {
        public byte nFieldType;    //字段类型
        public short nPosition;    //所在位置
        public int iLen;        //占用字节数(只用2个字节，但是short类型最大只能到+32767)
        public Object oField;    //内容
    }
}

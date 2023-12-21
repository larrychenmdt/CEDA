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

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public final class BufferByte
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(BufferByte.class);

    private static final int INCREASE_SIZE = 131070;    //扩大到2倍最大接收缓存

    //各种数据类型的长度
    private static final int LENGTH_INT = 4;
    private static final int LENGTH_LONG = 8;
    private static final int LENGTH_FLOAT = 4;
    private static final int LENGTH_DOUBLE = 8;
    private static final int LENGTH_SHORT = 2;
    private static final int LENGTH_CHAR = 2;
    private static final int LENGTH_BYTE = 1;
    private final byte[] bt1 = new byte[1];
    private final byte[] bt4 = new byte[4];
    private final byte[] bt2 = new byte[2];
    private final byte[] bt8 = new byte[8];
    private int iIncreaseSize = INCREASE_SIZE;
    private int iSize = 0;
    private int iReadCurPos = 0;            //当前读取内存页的位置
    private int iReadCurSize = 0;            //当前读取内存总位置
    private int iWriteCurPos = 0;            //当前写入内存页的位置
    private byte[] buff = null;

    //以指定增量扩展内存
    public BufferByte(int iIncreaseSize)
    {
        if (iIncreaseSize > 0) {
            this.iIncreaseSize = iIncreaseSize;
        }

        //m_arrPages = new Vector<byte[]>();

        //开辟内存
        //m_arrPages.addElement(new byte[m_iIncreaseSize]);
        buff = new byte[this.iIncreaseSize];
    }

    public BufferByte()
    {
        this(INCREASE_SIZE);    //默认以4K内存增长
        //System.out.println(" BufferByte new");
    }

    //把内存放入内存页
    public BufferByte(byte[] btIn)
    {
        this(INCREASE_SIZE);

        if (btIn == null || btIn.length == 0) {
            logger.error("BufferByte(byte[] btIn): byte[] is null or size=0");
            return;
        }

        put(btIn);
    }

    public void close()
    {
        //System.out.println(" BufferByte close");
        buff = null;
    }

    //返回对应位置的字节
    public byte getByte(int iIndex)
    {
        if (iSize == 0 || iIndex > iSize) {
            logger.error("BufferByte getByte(): size=0 or index>size");
            return 0;
        }

        //int iPage = iIndex / m_iIncreaseSize;//得到内存页编号
        int iPos = iIndex % iIncreaseSize;    //得到内存位置编号
        //byte[] bt = m_arrPages.elementAt(iPage);//得到对应位置的内容

        return buff[iPos];
    }

    public void get(byte[] dst)
    {
        if (iSize == 0 || dst == null) {
            logger.error("BufferByte get(): size=0 or byte[] is null");
            return;
        }

        int iLen = dst.length;
        //byte[] bt = null;

        if (iReadCurSize == iSize || ((iSize - iReadCurSize) < iLen)) {  //如果已经到达末尾,剩余内存不够
            logger.error("BufferByte get(): Current size is end or rest size is less than byte size");
            return;
        }

        if ((iIncreaseSize - iReadCurPos) >= iLen) { //如果内存都在本内存页
            //bt = m_arrPages.elementAt(m_iReadCurPage);//得到对应位置的内容
            System.arraycopy(buff, iReadCurPos, dst, 0, iLen);
            iReadCurPos += iLen;
        }
        else { //如果内存都在本内存页和下一个内存页
            logger.error("BufferByte get(): Current size is end or rest size is less than byte size");
            return;
        }

        iReadCurSize += iLen;
    }

    public void put(byte[] src)
    {
        if (src == null) {
            logger.error("BufferByte put(): byte[] is null");
            return;
        }

        //byte bt[] = null;
        int iLen = src.length;

        //如果此页放不下内容，则新开内存页
        if ((iLen + iWriteCurPos) > iIncreaseSize) {
            logger.error("BufferByte put(): Current size is end or rest size is less than byte size");
            return;
        }
        else { //保存到本内存页
            //bt = m_arrPages.elementAt(m_iWriteCurPage);
            System.arraycopy(src, 0, buff, iWriteCurPos, iLen);
            iWriteCurPos += iLen;
        }

        //m_iWriteCurSize += iLen;
        iSize += iLen;
    }

    public void put(byte[] src, int iStartOffset, int iCopyLen)
    {
        if (src == null) {
            logger.error("BufferByte put(): byte[] is null");
            return;
        }

        //byte bt[] = null;
        int iLen = src.length;

        if ((iLen - iStartOffset) < iCopyLen) {
            iLen = iLen - iStartOffset;
        }
        else {
            iLen = iCopyLen;
        }

        //如果此页放不下内容，则新开内存页
        if ((iLen + iWriteCurPos) > iIncreaseSize) {
            logger.error("BufferByte put(): Current size is end or rest size is less than byte size");
            return;
        }
        else { //保存到本内存页
            //bt = m_arrPages.elementAt(m_iWriteCurPage);
            System.arraycopy(src, iStartOffset, buff, iWriteCurPos, iLen);
            iWriteCurPos += iLen;
        }

        //m_iWriteCurSize += iLen;
        iSize += iLen;
    }

    public byte get()
    {
        //byte bt[] = new byte[LENGTH_BYTE];
        byte[] bt = bt1;

        get(bt);

        return bt[0];
    }

    public void putChar(char value)
    {
        put(TypeConverter.char2byte(value));
    }

    public char getChar()
    {
        //byte[] bt = new byte[LENGTH_CHAR];
        byte[] bt = bt2;
        get(bt);

        return TypeConverter.byte2char(bt);
    }

    public void putDouble(double value)
    {
        put(TypeConverter.double2byte(value));
    }

    public double getDouble()
    {
        //byte[] bt = new byte[LENGTH_DOUBLE];
        byte[] bt = bt8;
        get(bt);

        return TypeConverter.byte2double(bt);
    }

    public void putFloat(float value)
    {
        put(TypeConverter.float2byte(value));
    }

    public float getFloat()
    {
        //byte[] bt = new byte[LENGTH_FLOAT];
        byte[] bt = bt4;
        get(bt);

        return TypeConverter.byte2float(bt);
    }

    public void putInt(int value)
    {
        put(TypeConverter.int2byte(value));
    }

    public int getInt()
    {
        //byte[] bt = new byte[LENGTH_INT];
        byte[] bt = bt4;
        get(bt);

        return TypeConverter.byte2int(bt);
    }

    public void putLong(long value)
    {
        put(TypeConverter.long2byte(value));
    }

    public long getLong()
    {
        //byte[] bt = new byte[LENGTH_LONG];
        byte[] bt = bt8;
        get(bt);

        return TypeConverter.byte2long(bt);
    }

    public void putShort(short value)
    {
        put(TypeConverter.short2byte(value));
    }

    public void putUShort(int value)
    {
        put(TypeConverter.ushort2byte(value));
    }

    public short getShort()
    {
        //byte[] bt = new byte[LENGTH_SHORT];
        byte[] bt = bt2;
        get(bt);
        return TypeConverter.byte2short(bt);
    }
    public int getUShort()
    {
        //byte[] bt = new byte[4];
        //byte[] btTmp = new byte[2];
        byte[] bt = bt4;
        byte[] btTmp = bt2;
        get(btTmp);

        System.arraycopy(btTmp, 0, bt, 0, 2);
        bt[2] = 0;
        bt[3] = 0;
        return TypeConverter.byte2int(bt);
    }
    public void putByte(byte b)
    {
        //byte bt[] = new byte[LENGTH_BYTE];
        byte[] bt = bt1;
        bt[0] = b;
        put(bt);
    }

    public byte getByte()
    {
        //byte[] bt = new byte[LENGTH_BYTE];
        byte[] bt = bt1;
        get(bt);

        return bt[0];
    }

    public void clear()
    {
        iSize = 0;
        //m_iReadCurPage = 0;
        iReadCurPos = 0;
        //m_iWriteCurPage = 0;
        iWriteCurPos = 0;
        iReadCurSize = 0;
        //m_iWriteCurSize = 0;
    }

    public void rewind()
    {
        //m_iReadCurPage = 0;
        iReadCurPos = 0;
        iReadCurSize = 0;
    }

    public int size()
    {
        return iSize;
    }

    public int curReadPosition()
    {
        return iReadCurSize;
    }

    //转换为字节数组
    public byte[] convertBytes() throws Exception
    {
        if (iSize > Consts.MAX_MESSAGE_LENGTH) {
            throw new Exception("message is too large:" + iSize);
        }
        byte[] bt = new byte[iSize];
        System.arraycopy(buff, 0, bt, 0, iSize);
        return bt;
    }
}

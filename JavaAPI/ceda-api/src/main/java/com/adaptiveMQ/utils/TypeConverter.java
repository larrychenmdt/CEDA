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

public final class TypeConverter
{
    private TypeConverter() {};
    //各种数据类型的长度
    private static final int LENGTH_INT = 4;
    private static final int LENGTH_LONG = 8;
    private static final int LENGTH_SHORT = 2;
    private static final int LENGTH_CHAR = 2;

    public static byte[] char2byte(char c)
    {
        byte[] bt = new byte[LENGTH_CHAR];

        bt[0] = (byte) (c & 0xff);
        bt[1] = (byte) (c >> 8 & 0xff);

        return bt;
    }

    public static char byte2char(byte[] bt)
    {
        char c;

        c = (char) (bt[0] & 0x00ff + ((bt[1] << 8) & 0xff00));

        return c;
    }

    public static byte[] short2byte(short sValue)
    {
        byte[] btValue = new byte[LENGTH_SHORT];
        int shift;
        int i;

        for (i = 1, shift = 8; i >= 0; i--, shift -= 8) {
            btValue[i] = (byte) (0xFF & (sValue >> shift));
        }

        return btValue;
    }

    public static byte[] ushort2byte(int sValue)
    {
        byte[] btValue = new byte[LENGTH_SHORT];
        int shift;
        int i;

        for (i = 1, shift = 8; i >= 0; i--, shift -= 8) {
            btValue[i] = (byte) (0xFF & (sValue >> shift));
        }

        return btValue;
    }

    public static short byte2short(byte[] btValue)
    {
        short sValue = 0;

        if (btValue == null || btValue.length != LENGTH_SHORT) {
            return 0;
        }

        sValue += (btValue[1] << 8) & 0xff00;
        sValue += (btValue[0] << 0) & 0x00ff;

        return sValue;
    }

    public static int byte2ushort(byte[] btValue)
    {
        int sValue = 0;

        if (btValue == null || btValue.length != LENGTH_SHORT) {
            return 0;
        }

        sValue += (btValue[1] << 8) & 0xff00;
        sValue += (btValue[0] << 0) & 0x00ff;

        return sValue;
    }

    public static byte[] int2byte(int intValue)
    {
        byte[] btValue = new byte[LENGTH_INT];
        int shift;
        int i;
        //for(i = 0, shift = 24; i < 4; i++, shift -= 8)
        for (i = 3, shift = 24; i >= 0; i--, shift -= 8) {
            btValue[i] = (byte) (0xFF & (intValue >> shift));
        }

        return btValue;
    }

    public static int byte2int(byte[] btValue)
    {
        int intValue = 0;

        if (btValue == null || btValue.length != LENGTH_INT) {
            return 0;
        }

        intValue += (btValue[3] << 24) & 0xff000000;
        intValue += (btValue[2] << 16) & 0x00ff0000;
        intValue += (btValue[1] << 8) & 0x0000ff00;
        intValue += (btValue[0] << 0) & 0x000000ff;

        return intValue;
    }

    public static byte[] long2byte(long lValue)
    {
        byte[] btValue = new byte[LENGTH_LONG];
        int shift;
        int i;
        for (i = 7, shift = 56; i >= 0; i--, shift -= 8) {
            btValue[i] = (byte) (0xFF & (lValue >> shift));
        }

        return btValue;
    }

    public static long byte2long(byte[] btValue)
    {
        long lValue = 0;

        if (btValue == null || btValue.length != LENGTH_LONG) {
            return 0;
        }

        long lTmp = 0;
        lTmp = btValue[7];
        lValue += (lTmp << 56) & 0xff00000000000000L;
        lTmp = btValue[6];
        lValue += (lTmp << 48) & 0x00ff000000000000L;
        lTmp = btValue[5];
        lValue += (lTmp << 40) & 0x0000ff0000000000L;
        lTmp = btValue[4];
        lValue += (lTmp << 32) & 0x000000ff00000000L;
        lTmp = btValue[3];
        lValue += (lTmp << 24) & 0x00000000ff000000L;
        lTmp = btValue[2];
        lValue += (lTmp << 16) & 0x0000000000ff0000L;
        lTmp = btValue[1];
        lValue += (lTmp << 8) & 0x000000000000ff00L;
        lTmp = btValue[0];
        lValue += (lTmp << 0) & 0x00000000000000ffL;

        return lValue;
    }

    public static byte[] float2byte(float fValue)
    {
        return int2byte(Float.floatToIntBits(fValue));
    }

    public static float byte2float(byte[] btValue)
    {
        return Float.intBitsToFloat(byte2int(btValue));
    }

    public static byte[] double2byte(double dValue)
    {
        return long2byte(Double.doubleToLongBits(dValue));
    }

    public static double byte2double(byte[] btValue)
    {
        return Double.longBitsToDouble(byte2long(btValue));
    }
}

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public final class Utils
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Utils.class);

    static final String WILDCARD = "*";
    static final String ALL_WILDCARD = "**";

    private static final String STR_UTF8 = "utf-8";

    @Deprecated
    public static String displayBuffer(BufferByte bb)
    {
        StringBuffer sz = new StringBuffer();

        int n = 0;
        bb.rewind();
        for (int i = 0; i < bb.size(); i++) {
            int v = bb.getByte(i) & 0xFF;
            sz.append(((v <= 15) ? "0" : "") + Integer.toHexString(v).toUpperCase() + " ");
            if (0 == (++n % 16)) {
                sz.append("\n");
            }
        }

        return sz.toString();
    }

    @Deprecated
    public static String displayBuffer(ByteBuffer b)
    {
        ByteBuffer bb = b.duplicate();
        StringBuffer sz = new StringBuffer();

        int n = 0;
        bb.rewind();
        while (bb.hasRemaining()) {
            int v = bb.get() & 0xFF;
            sz.append(((v <= 15) ? "0" : "") + Integer.toHexString(v).toUpperCase() + " ");
            if (0 == (++n % 16)) {
                sz.append("\n");
            }
        }

        return sz.toString();
    }

    @Deprecated
    public static String displayBuffer(byte[] b)
    {
        BufferByte bb = new BufferByte(b);

        return displayBuffer(bb);
    }

    //将字符串形数字转换为int形
    @Deprecated
    public static int str2int(String strNumber)
    {
        if (strNumber == null || strNumber.equals("")) {
            return 0;
        }

        return Integer.valueOf(strNumber).intValue();
    }

    //将字符串形数字转换为float形
    @Deprecated
    public static float str2float(String strNumber)
    {
        if (strNumber == null || strNumber.equals("")) {
            return 0f;
        }

        return Float.valueOf(strNumber).floatValue();
    }

    //将字符串形数字转换为double形
    @Deprecated
    public static double str2double(String strNumber)
    {
        if (strNumber == null || strNumber.equals("")) {
            return 0f;
        }

        return Double.valueOf(strNumber).doubleValue();
    }

    @Deprecated
    public static String getString(String strIn)
    {
        if (strIn == null) {
            return "";
        }
        return strIn;
    }

    public static String getSysString(byte[] bdata) throws UnsupportedEncodingException
    {
        return new String(bdata, StandardCharsets.UTF_8);
        /*
        try {
            String sret= new String(bdata,STR_UTF8);
            return sret;
        } catch (Exception e) {
            // TODO: handle exception
            return null;
        }
        */
    }

    public static byte[] getSysByte(String str) throws UnsupportedEncodingException
    {
        return str.getBytes(StandardCharsets.UTF_8);
        //return bret;
        /*
        try {
            byte[] bret=str.getBytes(STR_UTF8);
            return bret;
        } catch (Exception e) {
            // TODO: handle exception
            return null;
        }
        */

    }

    public static int byteToInt(byte[] data, int length)
    {
        int result = 0;
        for (int i = length - 1, j = 0; i < data.length && i >= 0 && j < length && j < 4; i--, j++) {
            result <<= 8;
            result |= (data[i] & 0x000000FF);
            //System.out.printf("%08X\n", result);
        }
        //System.out.printf("%08X\n", result);
        return result;
    }

    public static byte[] intToByte(int num, int size)
    {
        byte[] buf = new byte[Math.min(size, 4)];
        //for (int i = buf.length - 1; i >= 0; i--) {
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (byte) (num | 0x00);
            num >>= 8;
        }
        return buf;
    }

    public static boolean isWildcard(String topic)
    {
        return topic.indexOf("*") > -1;

    }

    //判断地址是否符合
    public static boolean matchDestination(String szWildDest, String szDest)
    {
        boolean bMatched = false;
        if (szWildDest.equals(szDest)) {
            return true;
        }
        //if(sz)
        /*
        //比较消息类型是否一致
        if (!szWildDest.substring(0, 2).equals(szDest.substring(0, 2)))
            return false;
        //全配符,可以符合任何地址
        if (szWildDest.substring(2, szWildDest.length()).equals(ALL_WILDCARD)) //地址要去掉前2位类型标示位
            return true;
        StringTokenizer wildPart = new StringTokenizer(szWildDest.substring(2,
                szWildDest.length()), ".");
        StringTokenizer destPart = new StringTokenizer(szDest.substring(2,
                szDest.length()), ".");
         */
        StringTokenizer wildPart = new StringTokenizer(szWildDest, ".");
        StringTokenizer destPart = new StringTokenizer(szDest, ".");

        int tokenCountDiff = destPart.countTokens() - wildPart.countTokens();

        if ((tokenCountDiff == 0) || (tokenCountDiff == -1) || szWildDest.indexOf(ALL_WILDCARD) >= 0) {
            while (wildPart.hasMoreElements() && destPart.hasMoreElements()) {
                String szWildDescPart = (String) wildPart.nextElement();
                String szDescPart = (String) destPart.nextElement();

                if (szWildDescPart.equals(WILDCARD)) { //通配符*
                    bMatched = true;
                    continue;
                }
                else if (szWildDescPart.equals(ALL_WILDCARD)) { //全配符**
                    bMatched = true;
                    break;
                }
                else if (szWildDescPart.equals(szDescPart)) { //此部分一致
                    bMatched = true;
                    continue;
                }
                else {
                    bMatched = false;
                    break;
                }
            }
        }

        wildPart = null;
        destPart = null;

        return bMatched;
    }

    @Deprecated
    public static byte[] zlibStr(String instr)
    {
        if (instr == null || instr.length() == 0) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            DeflaterOutputStream zlip = new DeflaterOutputStream(out);
            zlip.write(instr.getBytes(StandardCharsets.UTF_8));

            zlip.close();
            return out.toByteArray();
        }
        catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    /**
     * 获取字符串gzip后内容，使用UTF-8编码
     *
     * @param String instr：待压缩字符串
     * @return byte[]：压缩后的字节数组
     * @throws
     */
    public static byte[] gzipStr(String instr)
    {
        byte[] bout = null;
        if (instr == null || instr.length() == 0) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            GZIPOutputStream gzip = new GZIPOutputStream(out);
            gzip.write(instr.getBytes(StandardCharsets.UTF_8));

            gzip.close();
            bout = out.toByteArray();
            out.close();
            gzip = null;
        }
        catch (Exception e) {
            logger.error(e);
        }
        out = null;
        return bout;
    }

    /**
     * 获取解压后字符串后内容，使用UTF-8编码
     *
     * @param byte[] inByte：压缩的字节数组
     * @return String：解压后的字符串
     * @throws
     */
    public static String ungzipByte(byte[] inByte)
    {
        String sout = null;
        //ArrayByte aByte=getFreeByte();
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(inByte);
            GZIPInputStream gzin = new GZIPInputStream(in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            byte[] buf = new byte[4096];

            int nlen = -1;
            while ((nlen = gzin.read(buf, 0, buf.length)) > -1) {
                out.write(buf, 0, nlen);
            }
            gzin.close();

            byte[] bout = out.toByteArray();
            out.close();
            sout = new String(bout, StandardCharsets.UTF_8);
            gzin = null;
            out = null;
            buf = null;

        }
        catch (Exception e) {
            logger.error(e);
        }
        //aByte.bUsed=false;
        return sout;
    }
    @Deprecated
    public static String unzlibByte(byte[] inByte)
    {
        String sout = null;
        //ArrayByte aByte=getFreeByte();
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(inByte);
            InflaterInputStream zlib = new InflaterInputStream(in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];

            int nlen = -1;
            while ((nlen = zlib.read(buf, 0, buf.length)) > -1) {
                out.write(buf, 0, nlen);
            }
            zlib.close();

            byte[] bout = out.toByteArray();
            out.close();
            sout = new String(bout, StandardCharsets.UTF_8);
            //sout=  new String(bout,"UTF-8");

        }
        catch (Exception e) {
            logger.error(e);
        }
        //aByte.bUsed=false;
        return sout;

    }

    /**
     * 获取本机HostName
     *
     * @param null
     * @return String：本机HostName
     * @throws
     */
    public String getHostName()
    {
        String sret = null;
        try {
            sret = InetAddress.getLocalHost().getHostName();

        }
        catch (Exception e) {
            logger.error(e);
        }
        return sret;
    }
}

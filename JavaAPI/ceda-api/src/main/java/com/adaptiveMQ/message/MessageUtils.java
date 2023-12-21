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

import com.adaptiveMQ.message.MessageRecord.CField;
import com.adaptiveMQ.utils.ConstsMessage;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Iterator;
import java.util.List;

public final class MessageUtils
{
    private  MessageUtils() {}

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageUtils.class);

    public static void copyRecord(MessageRecord pOriginRecord, MessageRecord pNewRecord)
    {
        Iterator<CField> it = pOriginRecord.getFieldIterator();
        try {
            while (it.hasNext()) {
                CField field = it.next();
                pNewRecord.addField(field.nPosition, field);
            }
        }
        catch (Exception e) {
            logger.error("copyRecord fail:", e);
        }
    }

    public static ControlMessage getVsLogin(Message msg)
    {
        ControlMessage ctlMsg = new ControlMessage();
        ctlMsg.setInterMsgType(ConstsMessage.MSG_TYPE_CTRL_LOGIN);
        ctlMsg.setControlCode(ConstsMessage.CTRL_CODE_LOGIN_VS);
        copyRecord(msg.getMessageBody(), ctlMsg.getMessageBody());
        return ctlMsg;
    }

    //把用户信息转换为数据流
    public static ControlMessage getUserInfo(String szUsername, String szPassword, String szClientID, int nLoginID)
    {
        if (szUsername == null || szPassword == null || szClientID == null) {
            logger.error("CMessageUtils getUserInfo: Parameter is NULL");
            return null;
        }

        ControlMessage ctlMsg = new ControlMessage();
        MessageBody body = ctlMsg.getMessageBody();
        try {
            body.addString((short) 4, szUsername);
            body.addString((short) 5, szPassword);
            body.addString((short) 7, szClientID);
        }
        catch (Exception e) {
            logger.error("CMessageUtils getUserInfo failed", e);
            return null;
        }

        ctlMsg.setInterMsgType(ConstsMessage.MSG_TYPE_CTRL_LOGIN);
        ctlMsg.setControlCode(ConstsMessage.CTRL_CODE_LOGIN);

        return ctlMsg;
    }

    //把用户信息转换为ValidationSvr登陆数据流
    public static ControlMessage getSIDLoginInfo(String szSID, int nLoginID)
    {
        if (szSID == null) {
            logger.error("CMessageUtils getSIDLoginInfo: Parameter is NULL");
            return null;
        }

        ControlMessage ctlMsg = new ControlMessage();
        ctlMsg.setInterMsgType(ConstsMessage.MSG_TYPE_CTRL_LOGIN);
        ctlMsg.setControlCode(ConstsMessage.CTRL_CODE_LOGIN_SID);
        try {
            ctlMsg.getMessageBody().addString((short) 3, szSID);
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }

        //把LoginConnID放倒登录的后面

        //ctlMsg.setCommand(btCmd);

        return ctlMsg;
    }

    //把地址转换为数据流
    public static ControlMessage getBatchDestination(List<BaseDestination> destinationList, byte msgCode, byte nConstrolCode)
    {
        if (destinationList.size() < 1) {
            logger.error("CMessageUtils getDestination: CDestination is NULL");
            return null;
        }

        StringBuffer sbuff = new StringBuffer();

        int nsize = destinationList.size();

        for (int i = 0; i < nsize; i++) {
            BaseDestination des = destinationList.get(i);
            sbuff.append(des.getName());
            if (i < (nsize - 1)) {
                sbuff.append("|");
            }
        }

        ControlMessage ctlMsg = new ControlMessage();
        ctlMsg.setInterMsgType(msgCode);
        ctlMsg.setControlCode(nConstrolCode);
        try {
            ctlMsg.getMessageBody().addString((short) 3, sbuff.toString());
        }
        catch (Exception e) {
            logger.error(e);
            return null;
        }

        return ctlMsg;
    }

    //解析登陆结果
    public static LoginResult parseLoginResult(MessageBody body)
    {
        if (body == null) {
            logger.error("CMessageUtils parseLoginResult: Parameter is NULL");
            return null;
        }

        LoginResult oCmdResult = new LoginResult();
        try {
            oCmdResult.nCode = body.getInt((short) 1);
            if (oCmdResult.nCode == 1) {
                oCmdResult.nConnID = body.getInt((short) 6);
            }
            else {
                oCmdResult.szCmdString = body.getString((short) 4);
            }
        }
        catch (Exception e) {
            oCmdResult.nConnID = 0;
            logger.error("parseLoginResult fail:", e);
        }
        return oCmdResult;
    }

    //将long型日期时间转换为字符串
    public static String parseDateTime(long lDateTime)
    {
        int nYear;
        int nMonth;
        int nDay;
        int nHour;
        int nMin;
        int nSec;
        long lRestTime;
        String strDatetime = null;

        lRestTime = lDateTime;

        nYear = (int) (lRestTime / 10000000000L);
        lRestTime = lRestTime % 10000000000L;

        nMonth = (int) (lRestTime / 100000000L);
        lRestTime = lRestTime % 100000000L;

        nDay = (int) (lRestTime / 1000000L);
        lRestTime = lRestTime % 1000000L;

        nHour = (int) (lRestTime / 10000L);
        lRestTime = lRestTime % 10000L;

        nMin = (int) (lRestTime / 100);
        nSec = (int) (lRestTime % 100);

        strDatetime = nYear + "/";

        if (nMonth < 10) {
            strDatetime += "0";
        }

        strDatetime += nMonth;
        strDatetime += "/";

        if (nDay < 10) {
            strDatetime += "0";
        }
        strDatetime += nDay;
        strDatetime += " ";

        if (nHour < 10) {
            strDatetime += "0";
        }
        strDatetime += nHour;
        strDatetime += ":";

        if (nMin < 10) {
            strDatetime += "0";
        }
        strDatetime += nMin;
        strDatetime += ":";

        if (nSec < 10) {
            strDatetime += "0";
        }
        strDatetime += nSec;

        return strDatetime;
    }

    //握手结果类
    public static class ShakehandsResult
    {
        public String szVersion;
        public short nConnID;
        public long lServerTime;

        public String toString()
        {
            return szVersion + ", Connection ID[" + nConnID + "], Login Time: " + parseDateTime(lServerTime);
        }
    }

    //登陆和订阅结果类
    public static class CmdResult
    {
        public byte nCode;
        public String szCmdString;
    }

    //登陆结果类
    public static class LoginResult
    {
        public int nCode = 0;
        public String szCmdString;
        public int nConnID;
    }
}

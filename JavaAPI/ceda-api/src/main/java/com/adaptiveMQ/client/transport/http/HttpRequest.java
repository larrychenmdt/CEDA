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

package com.adaptiveMQ.client.transport.http;

import com.adaptiveMQ.message.internal.MessageConverter;
import com.adaptiveMQ.utils.Utils;
import java.io.UnsupportedEncodingException;

public final class HttpRequest
{
    private static final String WBS_VERSION = "Sec-WebSocket-Version: 13";
    private static final String CONN_UPATE = "Connection: Upgrade";
    private static final String UPATE_WEB = "Upgrade: WebSocket";
    private static final String ORIGIN_HTTP_STR = "Origin: http://";
    private static final String HOST_STR = "Host";
    private static final String SEC_KEY = "Sec-WebSocket-Key";
    private static final String wline = "\r\n";
    public static final String ACTION_GET = "GET";
    public static final String ACTION_POST = "POST";

    private StringBuffer sHeader = null;
    private byte[] body = null;
    public HttpRequest(String location, String sAction)
    {
        sHeader = new StringBuffer();
        sHeader.append(sAction);
        sHeader.append(" ");
        sHeader.append(location);
        sHeader.append(" HTTP/1.1");
        sHeader.append(wline);
    }
    private String getURL()
    {
        return null;
    }

    public void addKeyValue(String skey, String svalue)
    {
        sHeader.append(skey);
        sHeader.append(": ");
        sHeader.append(svalue);
        sHeader.append(wline);
    }

    public void addLine(String svalue)
    {
        sHeader.append(svalue);
        sHeader.append(wline);
    }

    public void setBody(byte[] bbody)
    {
        body = bbody;
    }

    public byte[] getWsContent(String sHostPort) throws UnsupportedEncodingException
    {
        addLine(CONN_UPATE);
        addLine(UPATE_WEB);
        addLine(WBS_VERSION);
        addKeyValue(HOST_STR, sHostPort);
        sHeader.append(ORIGIN_HTTP_STR);
        addLine(sHostPort);
        addKeyValue(SEC_KEY, MessageConverter.getWebSocketKey());
        sHeader.append(wline);
        String sheader = sHeader.toString();
        return Utils.getSysByte(sheader);
    }

    public byte[] getContent() throws UnsupportedEncodingException
    {
        addKeyValue("Accept", "www/source;text/html;image/gif;image/jpeg;*/*");
        addKeyValue("User-Agent", "Mozilla/4.0");
        addKeyValue("Host", "127.0.0.1:443");
        addKeyValue("Content-Type", "multipart/form-data; boundary=---------------------------7d5d116460764");
        addKeyValue("Content-Disposition", " form-data; name='CDISO' haha");
        addKeyValue("Connection", "keep-alive");
        sHeader.append("\r\n");
        String sheader = sHeader.toString();
        return Utils.getSysByte(sheader);
    }
}

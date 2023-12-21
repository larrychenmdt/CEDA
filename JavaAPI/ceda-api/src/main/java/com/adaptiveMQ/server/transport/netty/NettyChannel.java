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

package com.adaptiveMQ.server.transport.netty;

import com.adaptiveMQ.server.transport.IChannel;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

public class NettyChannel implements IChannel
{
    private Channel channel = null;

    NettyChannel(Channel channel)
    {
        this.channel = channel;
    }

    @Override
    public void write(byte[] msg)
    {
        channel.writeAndFlush(Unpooled.wrappedBuffer(msg));
    }

    @Override
    public String remoteUrl()
    {
        return channel.remoteAddress().toString();
    }

    @Override
    public void close()
    {
         channel.close();
    }
}

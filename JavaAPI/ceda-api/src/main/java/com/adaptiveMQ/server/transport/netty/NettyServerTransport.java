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

import com.adaptiveMQ.cluster.ServiceInfo;
import com.adaptiveMQ.server.IServerLoginHandler;
import com.adaptiveMQ.server.IServerConnectionListener;
import com.adaptiveMQ.server.internal.ConnectionPoint;
import com.adaptiveMQ.server.internal.ConnectionPointManager;
import com.adaptiveMQ.server.MQServer;
import com.adaptiveMQ.server.transport.IServerTransport;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class NettyServerTransport implements IServerTransport
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MQServer.class);
    private final ConcurrentHashMap<ChannelHandlerContext, ConnectionPoint> nettyChannelMap = new ConcurrentHashMap<>();
    private InetSocketAddress serverAddress = null;
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private IServerConnectionListener serverConnectionListener;
    private IServerLoginHandler serverConnectionAuth;
    private ConnectionPointManager connectionPointManager;

    @Override
    public void initialize(ConnectionPointManager connectionPointManager)
    {
        this.connectionPointManager = connectionPointManager;
    }

    @Override
    public void setServerConnectionListener(IServerConnectionListener serverConnectionListener)
    {
        this.serverConnectionListener = serverConnectionListener;
    }

    @Override
    public void setServerLoginHandler(IServerLoginHandler serverConnectionAuth)
    {
        this.serverConnectionAuth = serverConnectionAuth;
    }

    @Override
    public void bind(ServiceInfo serviceInfo)
    {
        serverAddress = new InetSocketAddress(serviceInfo.getHost(), serviceInfo.getPort());
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        serverBootstrap = new io.netty.bootstrap.ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childOption(ChannelOption.SO_KEEPALIVE, true).handler(new LoggingHandler(LogLevel.DEBUG)).childHandler(new ChannelInitializer<SocketChannel>()
        {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception
            {
                try {
                    socketChannel.pipeline().addLast(new NettyChannelHandler());
                }
                catch (Exception e) {
                    logger.error("socketChannel.pipeline addLast failed", e);
                }
            }
        });
        try {
            io.netty.channel.ChannelFuture cf = serverBootstrap.bind(serverAddress);
            // svrChannel = cf.channel();
        }
        catch (Exception e) {
            logger.error("can't bind host:" + serverAddress.getHostName() + ", port:" + serverAddress.getPort(), e);
            System.exit(0);
        }
    }

    private final class NettyChannelHandler extends SimpleChannelInboundHandler<ByteBuf>
    {
        @Override
        public void exceptionCaught(io.netty.channel.ChannelHandlerContext ctx, Throwable e) throws Exception
        {
            ctx.channel().close();
        }

        @Override
        public void channelActive(io.netty.channel.ChannelHandlerContext ctx) throws Exception
        {
            String address = ctx.channel().remoteAddress().toString();
            logger.info("connect from: " + address);
            nettyChannelMap.put(ctx, new ConnectionPoint(address, new NettyChannel(ctx.channel()), serverConnectionListener, serverConnectionAuth, connectionPointManager));
        }

        @Override
        public void channelInactive(io.netty.channel.ChannelHandlerContext ctx) throws Exception
        {
            logger.info("disconnect from: " + ctx.channel().remoteAddress().toString());
            ConnectionPoint point = nettyChannelMap.get(ctx);
            if (point != null) {
                nettyChannelMap.remove(ctx);
                connectionPointManager.disConnect(point);
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception
        {
            byte[] reqByte = new byte[msg.readableBytes()];
            msg.readBytes(reqByte);
            ConnectionPoint point = nettyChannelMap.get(ctx);
            if (point != null) {
                point.inPutData(reqByte, reqByte.length);
            }
        }
    }

    @Override
    public void stop() {}
}

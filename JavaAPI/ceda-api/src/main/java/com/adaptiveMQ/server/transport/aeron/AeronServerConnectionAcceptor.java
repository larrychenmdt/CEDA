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

package com.adaptiveMQ.server.transport.aeron;

import com.adaptiveMQ.cluster.ServiceInfo;
import com.adaptiveMQ.exceptions.CedaException;
import com.adaptiveMQ.server.transport.IChannelInboundHandler;
import com.adaptiveMQ.utils.Consts;
import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;
import io.aeron.archive.codecs.AuthConnectRequestDecoder;
import io.aeron.archive.codecs.MessageHeaderDecoder;
import io.aeron.logbuffer.FragmentHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.agrona.concurrent.Agent;

import java.util.concurrent.TimeUnit;

import static com.adaptiveMQ.utils.Consts.AERON_SERVER_DEFAULT_ACCEPT_STREAM_ID;

public class AeronServerConnectionAcceptor implements Agent
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AeronServerConnectionAcceptor.class);
    private static final String FRAME_COUNT_LIMIT_PROP = "aeron.server.frameCountLimit";
    private static final int FRAGMENT_COUNT_LIMIT = Integer.getInteger(FRAME_COUNT_LIMIT_PROP, 10);

    private static final long CONNECT_TIMEOUT_DEFAULT_NS = TimeUnit.SECONDS.toNanos(5);
    private long connectTimeoutNs = CONNECT_TIMEOUT_DEFAULT_NS;

    private final FragmentAssembler assembler;
    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();
    private final AuthConnectRequestDecoder authConnectRequest = new AuthConnectRequestDecoder();

    private Aeron aeron;
    private ServiceInfo serviceInfo;
    private Subscription acceptSubscription;

    private IChannelInboundHandler channelInboundHandler;

    AeronServerConnectionAcceptor(Aeron aeron, ServiceInfo serviceInfo, IChannelInboundHandler channelInboundHandler)
    {
        this.aeron = aeron;
        this.serviceInfo = serviceInfo;
        this.channelInboundHandler = channelInboundHandler;
        assembler = new FragmentAssembler(onFragment(serviceInfo.getAeronAcceptStreamID()));
    }

    @Override
    public void onStart()
    {
        logger.info("aeron acceptor connected to context");
        String channel = String.format(Consts.getAeronChannelStringTemplate(serviceInfo.getProtocolType()), serviceInfo.getHost(), serviceInfo.getPort());
        logger.info(String.format("aeron acceptor add subscription:[%s]", channel));
        acceptSubscription = aeron.addSubscription(channel, AERON_SERVER_DEFAULT_ACCEPT_STREAM_ID);
    }

    @Override
    public int doWork() throws Exception
    {
        final int fragmentsRead = acceptSubscription.poll(assembler, FRAGMENT_COUNT_LIMIT);
        return fragmentsRead;
    }

    @Override
    public String roleName()
    {
        return "AeronServerConnectionAcceptor";
    }

    public FragmentHandler onFragment(final int streamId)
    {
        return (buffer, offset, length, header) -> {
            logger.debug(String.format("message to stream %d from session %x term id %x term offset %d (%d@%d)%n",
                    streamId, header.sessionId(), header.termId(), header.termOffset(), length, offset));

            final io.aeron.archive.codecs.MessageHeaderDecoder headerDecoder = messageHeaderDecoder;
            headerDecoder.wrap(buffer, offset);

            final int schemaId = headerDecoder.schemaId();

            logger.debug("received header:" + headerDecoder.toString());
            if (schemaId != io.aeron.archive.codecs.MessageHeaderDecoder.SCHEMA_ID) {
                throw new CedaException("expected schemaId=" + io.aeron.archive.codecs.MessageHeaderDecoder.SCHEMA_ID + ", actual=" + schemaId);
            }

            switch (messageHeaderDecoder.templateId()) {
                case AuthConnectRequestDecoder.TEMPLATE_ID: {

                    final AuthConnectRequestDecoder decoder = authConnectRequest;
                    decoder.wrap(buffer, offset + io.aeron.archive.codecs.MessageHeaderDecoder.ENCODED_LENGTH, headerDecoder.blockLength(), headerDecoder.version());

                    logger.debug("received AuthConnectRequest:" + decoder.toString());
                    final AeronServerConnection channel = new AeronServerConnection(aeron, channelInboundHandler);
                    channel.createPublication(decoder.responseChannel(), decoder.responseStreamId());

                    this.channelInboundHandler.channelActive(channel);
                    //TODO: order dependent, sync require sub channel which initialized in channelActive(...)
                    channel.sendbackConnectSyncAck();

                    logger.debug("send connect synchronized ack done");
                    break;
                }
            }
        };
    }

    @Override
    public void onClose()
    {
    }
}

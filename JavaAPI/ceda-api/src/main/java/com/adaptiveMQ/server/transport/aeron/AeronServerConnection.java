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

import com.adaptiveMQ.exceptions.CedaException;
import com.adaptiveMQ.server.transport.IChannel;
import com.adaptiveMQ.server.transport.IChannelInboundHandler;
import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.codecs.AuthConnectRequestEncoder;
import io.aeron.archive.codecs.MessageHeaderEncoder;
import io.aeron.logbuffer.FragmentHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.agrona.BitUtil;
import org.agrona.BufferUtil;
import org.agrona.concurrent.AgentInvoker;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.NanoClock;
import org.agrona.concurrent.SystemNanoClock;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.YieldingIdleStrategy;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AeronServerConnection implements AeronSession, IChannel
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AeronServerConnection.class);
    private static final int FRAGMENT_COUNT_LIMIT = 10;

    private AtomicBoolean running = new AtomicBoolean(true);
    private FragmentAssembler assembler;
    private IChannelInboundHandler channelInboundHandler;

    private final MessageHeaderEncoder messageHeader = new MessageHeaderEncoder();
    private final IdleStrategy retryIdleStrategy = YieldingIdleStrategy.INSTANCE;
    private NanoClock nanoClock = SystemNanoClock.INSTANCE;
    private static final long CONNECT_TIMEOUT_DEFAULT_NS = TimeUnit.SECONDS.toNanos(5);
    private long connectTimeoutNs = CONNECT_TIMEOUT_DEFAULT_NS;

    private final UnsafeBuffer buffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(1024, BitUtil.CACHE_LINE_LENGTH));
    private int pubStreamId;
    private String pubStreamChannel = null;
    private Publication publication = null;
    private int subStreamId;
    private String subStreamChannel = null;
    private Subscription subscription = null;
    private Aeron aeron;

    public AeronServerConnection(Aeron aeron, IChannelInboundHandler channelInboundHandler)
    {
        this.aeron = aeron;
        this.channelInboundHandler = channelInboundHandler;
    }

    public void createPublication(String pubStreamChannel, int pubStreamId)
    {
        logger.info(String.format("create publication, pubStreamChannel:%s, pubStreamId：%d",
                pubStreamChannel, pubStreamId));
        this.pubStreamId = pubStreamId;
        this.pubStreamChannel = pubStreamChannel;
        publication = aeron.addPublication(pubStreamChannel, pubStreamId);
    }

    public void createSubscription(String subStreamChannel, int subStreamId)
    {
        logger.info(String.format("create subscription, subStreamChannel:%s, subStreamId：%d",
                subStreamChannel, subStreamId));
        assembler = new FragmentAssembler(onFragment(subStreamId));
        this.subStreamId = subStreamId;
        this.subStreamChannel = subStreamChannel;
        subscription = aeron.addSubscription(subStreamChannel, subStreamId);
        assert subscription.isConnected();
    }

    public void createSubscription(String subStreamChannel)
    {
        //hack for IPC
        subStreamId = -pubStreamId;
        createSubscription(subStreamChannel, subStreamId);
    }

    public FragmentHandler onFragment(final int streamId)
    {
        return (buffer, offset, length, header) -> {
            logger.debug(String.format("message to stream %d from session %x term id %x term offset %d (%d@%d)%n",
                    streamId, header.sessionId(), header.termId(), header.termOffset(), length, offset));
            final byte[] data = new byte[length];
            buffer.getBytes(offset, data);
            channelInboundHandler.channelRead(this, data);
        };
    }

    @Override
    public int doWork()
    {
        final int fragmentsRead = subscription.poll(assembler, FRAGMENT_COUNT_LIMIT);
        return fragmentsRead;
    }

    public boolean sendbackConnectSyncAck()
    {
        final String responseChannel = subStreamChannel;
        final int responseStreamId = subStreamId;
        final AuthConnectRequestEncoder connectRequestEncoder = new AuthConnectRequestEncoder();
        connectRequestEncoder
                .wrapAndApplyHeader(buffer, 0, messageHeader)
                .correlationId(0)
                .responseStreamId(responseStreamId)
                .version(AeronArchive.Configuration.PROTOCOL_SEMANTIC_VERSION)
                .responseChannel(responseChannel);
        logger.info("send back connect synchronized ack, " + connectRequestEncoder.toString());
        return offerWithTimeout(connectRequestEncoder.encodedLength(), null);
    }

    private boolean offerWithTimeout(final int length, final AgentInvoker aeronClientInvoker)
    {
        retryIdleStrategy.reset();

        final long deadlineNs = nanoClock.nanoTime() + connectTimeoutNs;
        while (true) {
            final long result = publication.offer(buffer, 0, MessageHeaderEncoder.ENCODED_LENGTH + length);
            if (result > 0) {
                logger.debug("blocking offer success");
                return true;
            }
            if (result == Publication.CLOSED) {
                throw new CedaException("connection to the archive has been closed");
            }
            if (result == Publication.MAX_POSITION_EXCEEDED) {
                throw new CedaException("offer failed due to max position being reached");
            }
            if (deadlineNs - nanoClock.nanoTime() < 0) {
                logger.debug("blocking offer time out");
                return false;
            }
            if (null != aeronClientInvoker) {
                aeronClientInvoker.invoke();
            }
            retryIdleStrategy.idle();
        }
    }

    @Override
    public void write(byte[] messageBytes)
    {
        logger.debug(String.format("offer length:[%d]", messageBytes.length));

        buffer.putBytes(0, messageBytes);
        final long result = publication.offer(buffer, 0, messageBytes.length);

        if (result < 0L) {
            if (result == Publication.BACK_PRESSURED) {
                logger.warn(" Offer failed due to back pressure");
            }
            else if (result == Publication.NOT_CONNECTED) {
                logger.warn(" Offer failed because publisher is not connected to subscriber");
            }
            else if (result == Publication.ADMIN_ACTION) {
                logger.warn("Offer failed because of an administration action in the system");
            }
            else if (result == Publication.CLOSED) {
                logger.warn("Offer failed publication is closed");
            }
            else if (result == Publication.MAX_POSITION_EXCEEDED) {
                logger.warn("Offer failed due to publication reaching max position");
            }
            else {
                logger.warn(" Offer failed due to unknown reason");
            }
        }
        else {
            logger.debug(String.format("Offer success, position:[%d]", result));
        }
    }

    @Override
    public String remoteUrl()
    {
        return pubStreamChannel;
    }

    @Override
    public void abort()
    {
        close();
    }

    @Override
    public boolean isDone()
    {
        return !running.get();
    }

    @Override
    public long sessionId()
    {
        return pubStreamId;
    }

    @Override
    public void close()
    {
        running.set(false);
    }
}

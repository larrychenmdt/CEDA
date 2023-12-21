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

package com.adaptiveMQ.client.transport.aeron;

import com.adaptiveMQ.client.internal.MessageProcessor;
import com.adaptiveMQ.client.transport.AbstractWriteService;
import com.adaptiveMQ.exceptions.CedaException;
import com.adaptiveMQ.message.internal.MessageConverter;
import com.adaptiveMQ.server.transport.aeron.AeronServerConnection;
import com.adaptiveMQ.utils.BufferByte;
import io.aeron.Aeron;
import io.aeron.Publication;
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

public class AeronClientWriteService extends AbstractWriteService
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AeronServerConnection.class);

    private final UnsafeBuffer buffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(1024 * 1024 * 8, BitUtil.CACHE_LINE_LENGTH));
    private Aeron aeron;
    private Publication publication = null;
    private BufferByte msgBuf = new BufferByte();

    private final IdleStrategy retryIdleStrategy = YieldingIdleStrategy.INSTANCE;
    private NanoClock nanoClock = SystemNanoClock.INSTANCE;
    private static final long CONNECT_TIMEOUT_DEFAULT_NS = TimeUnit.SECONDS.toNanos(5);
    private long connectTimeoutNs = CONNECT_TIMEOUT_DEFAULT_NS;

    public AeronClientWriteService(MessageProcessor msgProcessor, Aeron aeron)
    {
        super(msgProcessor);
        this.aeron = aeron;
    }

    public void createPublication(String pubStreamChannel, int pubStreamId)
    {
        logger.info(String.format("create publication, pubStreamChannel:%s, pubStreamId：%d",
                pubStreamChannel, pubStreamId));
        publication = aeron.addPublication(pubStreamChannel, pubStreamId);
        assert publication.isConnected();
    }

    @Override
    public void output(byte[] btOutput, int size) throws Exception
    {
        byte[] btStreamOutput = MessageConverter.getWsStream(btOutput, msgBuf);
        logger.debug(String.format("offer length:[%d]", btStreamOutput.length));
        buffer.putBytes(0, btStreamOutput);
        offerWithTimeout(btStreamOutput.length, null);
    }

    private boolean offerWithTimeout(final int length, final AgentInvoker aeronClientInvoker)
    {
        retryIdleStrategy.reset();
        final long deadlineNs = nanoClock.nanoTime() + connectTimeoutNs;
        while (true) {
            final long result = publication.offer(buffer, 0, length);
            if (result > 0) {
                logger.debug("blocking offer success");
                return true;
            }
            if (result == Publication.CLOSED) {
                throw new CedaException("connection to the archive has been closed");
            }
            if (result == Publication.MAX_POSITION_EXCEEDED) {
                throw new CedaException(("offer failed due to max position being reached"));
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
    public void destory()
    {
    }

    @Override
    public void initWrite()
    {
    }
}

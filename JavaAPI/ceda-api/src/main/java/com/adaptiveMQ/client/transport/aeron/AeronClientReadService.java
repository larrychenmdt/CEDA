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

import com.adaptiveMQ.client.internal.MessageAppendHandler;
import com.adaptiveMQ.client.internal.MessageProcessor;
import com.adaptiveMQ.client.transport.IClientReadService;
import com.adaptiveMQ.message.IMessage;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.internal.MessageConverter;
import com.adaptiveMQ.utils.BufferByte;
import com.adaptiveMQ.utils.ConstsMessage;
import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;

public class AeronClientReadService implements IClientReadService, Agent
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AeronClientReadService.class);

    private int subStreamId;
    private String subStreamChannel = null;
    private Subscription subscription = null;
    private Aeron aeron;
    private FragmentAssembler assembler;
    private AgentRunner serviceAgentRunner;
    private final AeronClientConnection.Context ctx;
    private static final int FRAGMENT_COUNT_LIMIT = 10;
    private final BufferByte decodeBuffer = new BufferByte();
    private final MessageProcessor messageProcessor;
    private final MessageAppendHandler appendHandler;

    public Subscription getSubscription()
    {
        return subscription;
    }

    public int getSubStreamId()
    {
        return subStreamId;
    }

    public String getSubStreamChannel()
    {
        return subStreamChannel;
    }

    public AeronClientReadService(MessageProcessor messageProcessor, Aeron aeron, AeronClientConnection.Context ctx)
    {
        this.aeron = aeron;
        this.ctx = ctx;
        this.messageProcessor = messageProcessor;
        this.appendHandler= new MessageAppendHandler(messageProcessor);
    }

    @Override
    public void work()
    {
        serviceAgentRunner = new AgentRunner(ctx.serviceIdleStrategy(), ctx.errorHandler(), ctx.errorCounter(), this);
        AgentRunner.startOnThread(serviceAgentRunner, ctx.threadFactory());
    }

    public void createSubscription(String subStreamChannel, int subStreamId)
    {
        logger.info(String.format("create subscription, subStreamChannel:%s, subStreamId：%d",
                subStreamChannel, subStreamId));
        assembler = new FragmentAssembler(onFragment(subStreamId));
        this.subStreamId = subStreamId;
        this.subStreamChannel = subStreamChannel;
        subscription = aeron.addSubscription(subStreamChannel, subStreamId);
    }

    public FragmentHandler onFragment(final int streamId)
    {
        return (buffer, offset, length, header) -> {
            logger.debug(String.format("message to stream %d from session %x term id %x term offset %d (%d@%d)%n",
                    streamId, header.sessionId(), header.termId(), header.termOffset(), length, offset));
            final byte[] btInput = new byte[length];
            buffer.getBytes(offset, btInput);
            try {
                MessageConverter.StrByte2MsgOutput result =
                        MessageConverter.byte2VaildMsg(decodeBuffer, btInput, btInput.length);
                IMessage msg = result.oMessage;
                for (int i = 0; msg != null && i < result.iMessageNumber; i++) {
                    if (msg.getInterMsgType() < ConstsMessage.MSG_TYPE_DATA_PUB) {
                        messageProcessor.addInMessage(msg);
                    }
                    else {
                        appendHandler.addMessage((Message) msg);
                    }

                    msg = msg.linkMessage();
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        };
    }

    @Override
    public void onStart()
    {
    }

    @Override
    public int doWork() throws Exception
    {
        final int fragmentsRead = subscription.poll(assembler, FRAGMENT_COUNT_LIMIT);
        return fragmentsRead;
    }

    @Override
    public void onClose()
    {
    }

    @Override
    public String roleName()
    {
        return "AeronClientReadService";
    }

    @Override
    public void close()
    {

    }
}

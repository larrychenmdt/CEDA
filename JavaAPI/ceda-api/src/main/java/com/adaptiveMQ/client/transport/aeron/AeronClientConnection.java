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

import com.adaptiveMQ.client.ClientInfo;
import com.adaptiveMQ.client.ConnectionException;
import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.client.ProtocolType;
import com.adaptiveMQ.client.transport.AbstractClientConnection;
import com.adaptiveMQ.exceptions.CedaException;
import com.adaptiveMQ.server.transport.aeron.AeronServerTransport;
import com.adaptiveMQ.server.transport.aeron.AeronThreadingMode;
import com.adaptiveMQ.server.transport.aeron.AeronUtil;
import com.adaptiveMQ.utils.Consts;
import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.codecs.AuthConnectRequestDecoder;
import io.aeron.archive.codecs.AuthConnectRequestEncoder;
import io.aeron.archive.codecs.MessageHeaderDecoder;
import io.aeron.archive.codecs.MessageHeaderEncoder;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.security.CredentialsSupplier;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentInvoker;
import org.agrona.concurrent.CountedErrorHandler;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.NanoClock;
import org.agrona.concurrent.SystemNanoClock;
import org.agrona.concurrent.YieldingIdleStrategy;
import org.agrona.concurrent.status.AtomicCounter;
import org.agrona.concurrent.status.StatusIndicator;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static io.aeron.archive.ArchiveThreadingMode.DEDICATED;

public final class AeronClientConnection extends AbstractClientConnection
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AeronClientConnection.class);

    private static final int FRAGMENT_COUNT_LIMIT = 1;
    private final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(256);
    private final MessageHeaderEncoder messageHeader = new MessageHeaderEncoder();
    private final IdleStrategy retryIdleStrategy = YieldingIdleStrategy.INSTANCE;
    private NanoClock nanoClock = SystemNanoClock.INSTANCE;

    final Context ctx = new Context();
    private Publication publication;
    private CredentialsSupplier credentialsSupplier;

    private static final long CONNECT_TIMEOUT_DEFAULT_NS = TimeUnit.SECONDS.toNanos(5);
    private long connectTimeoutNs = CONNECT_TIMEOUT_DEFAULT_NS;

    private static final String CREDENTIALS_STRING_TEMPLATE = "username=%s&password=%s";

    final AuthConnectRequestDecoder authConnectRequest = new AuthConnectRequestDecoder();
    // private final FragmentAssembler fragmentAssembler = new FragmentAssembler(this::onFragment);
    private final MessageHeaderDecoder messageHeaderDecoder = new MessageHeaderDecoder();

    public enum HandShakeStatus
    {
        /**
         * uninitialized status,
         */
        INIT,

        /**
         * The active open is performed by the client sending a SYN to the server
         */
        SYNC,

        /**
         *  In response, the server replies with a SYN-ACK
         */
        SYNC_ACK,

        /**
         *  Finally, the client sends an ACK back to the server
         */
        ACK
    }
    private HandShakeStatus handShakeStatus = HandShakeStatus.INIT;

    public static final class Configuration
    {
        public static final String THREADING_MODE_PROP_NAME = "ceda.aeron.server.threading.mode";

        public static AeronThreadingMode threadingMode()
        {
            return AeronThreadingMode.valueOf(System.getProperty(THREADING_MODE_PROP_NAME, DEDICATED.name()));
        }

        /**
         * Default {@link IdleStrategy} to be used for the archive {@link Agent}s when not busy.
         */
        public static final String CEDA_SERVER_IDLE_STRATEGY_PROP_NAME = "ceda.server.aeron.idle.strategy";

        /**
         * Default {@link IdleStrategy} to be used for the archive {@link Agent}s when not busy.
         *
         * @see #CEDA_SERVER_IDLE_STRATEGY_PROP_NAME
         */
        public static final String DEFAULT_IDLE_STRATEGY = "org.agrona.concurrent.BackoffIdleStrategy";

        /**
         * Create a supplier of {@link IdleStrategy}s for the {@link #CEDA_SERVER_IDLE_STRATEGY_PROP_NAME}
         * system property.
         *
         * @param controllableStatus if a {@link org.agrona.concurrent.ControllableIdleStrategy} is required.
         * @return the new idle strategy {@link Supplier}.
         */
        public static Supplier<IdleStrategy> idleStrategySupplier(final StatusIndicator controllableStatus)
        {
            return () ->
            {
                final String name = System.getProperty(CEDA_SERVER_IDLE_STRATEGY_PROP_NAME, DEFAULT_IDLE_STRATEGY);
                return io.aeron.driver.Configuration.agentIdleStrategy(name, controllableStatus);
            };
        }

    }

    public static final class Context implements Cloneable
    {
        private Aeron aeron;
        private File archiveDir;

        // private String archiveDirectoryName = Archive.Configuration.archiveDirName();
        // private boolean deleteArchiveOnStart = Archive.Configuration.deleteArchiveOnStart();

        private AeronThreadingMode threadingMode = Configuration.threadingMode();
        private ThreadFactory threadFactory;

        private Supplier<IdleStrategy> serviceIdleStrategySupplier;
        private CountedErrorHandler countedErrorHandler;

        private ErrorHandler errorHandler;
        private AtomicCounter errorCounter;

        /**
         * Get the thread factory used for creating threads in {@link AeronThreadingMode#SHARED} and
         * {@link ArchiveThreadingMode#DEDICATED} threading modes.
         *
         * @return thread factory used for creating threads in SHARED and DEDICATED threading modes.
         */
        public ThreadFactory threadFactory()
        {
            return threadFactory;
        }

        /**
         * Set the thread factory used for creating threads in {@link AeronThreadingMode#SHARED} and
         * {@link AeronThreadingMode#DEDICATED} threading modes.
         *
         * @param threadFactory used for creating threads in SHARED and DEDICATED threading modes.
         * @return this for a fluent API.
         */
        public Context threadFactory(final ThreadFactory threadFactory)
        {
            this.threadFactory = threadFactory;
            return this;
        }

        /**
         * Get the error counter that will record the number of errors observed.
         *
         * @return the error counter that will record the number of errors observed.
         */
        public AtomicCounter errorCounter()
        {
            return errorCounter;
        }

        /**
         * Set the error counter that will record the number of errors observed.
         *
         * @param errorCounter the error counter that will record the number of errors observed.
         * @return this for a fluent API.
         */
        public Context errorCounter(final AtomicCounter errorCounter)
        {
            this.errorCounter = errorCounter;
            return this;
        }

        /**
         * Get the {@link ErrorHandler} to be used by the Archive.
         *
         * @return the {@link ErrorHandler} to be used by the Archive.
         */
        public ErrorHandler errorHandler()
        {
            return errorHandler;
        }

        /**
         * Set the {@link ErrorHandler} to be used by the Archive.
         *
         * @param errorHandler the error handler to be used by the Archive.
         * @return this for a fluent API
         */
        public Context errorHandler(final ErrorHandler errorHandler)
        {
            this.errorHandler = errorHandler;
            return this;
        }

        /**
         * Non-default for context.
         *
         * @param countedErrorHandler to override the default.
         * @return this for a fluent API.
         */
        public Context countedErrorHandler(final CountedErrorHandler countedErrorHandler)
        {
            this.countedErrorHandler = countedErrorHandler;
            return this;
        }

        /**
         * The {@link #errorHandler()} that will increment {@link #errorCounter()} by default.
         *
         * @return {@link #errorHandler()} that will increment {@link #errorCounter()} by default.
         */
        public CountedErrorHandler countedErrorHandler()
        {
            return countedErrorHandler;
        }

        /**
         * Get a new {@link IdleStrategy} for idling the service {@link Agent}.
         *
         * @return a new {@link IdleStrategy} for idling the service {@link Agent}.
         */
        public IdleStrategy serviceIdleStrategy()
        {
            return serviceIdleStrategySupplier.get();
        }

        void conclude()
        {
            if (null == errorHandler) {
                errorHandler = new AeronServerTransport.LoggingErrorHandler();
            }
            if (null == errorCounter) {
                errorCounter = aeron.addCounter(AeronServerTransport.Configuration.CEDA_ERROR_COUNT_TYPE_ID, "Ceda API Errors");
            }
            if (null == countedErrorHandler) {
                countedErrorHandler = new CountedErrorHandler(errorHandler, errorCounter);
            }
            if (null == serviceIdleStrategySupplier) {
                serviceIdleStrategySupplier = Configuration.idleStrategySupplier(null);
            }
            if (null == threadFactory) {
                threadFactory = Thread::new;
            }
        }
    }

    public AeronClientConnection(ClientInfo clientInfo)
    {
        super(clientInfo);
    }

    @Override
    public void start() throws IOException, ConnectionException
    {
        logger.info("start aeron connection");

        if (credentialsSupplier == null) {
            credentialsSupplier = new CredentialsSupplier()
            {
                public byte[] encodedCredentials()
                {
                    return String.format(CREDENTIALS_STRING_TEMPLATE, clientInfo.getUsername(), clientInfo.getPassword()).getBytes();
                }
                public byte[] onChallenge(final byte[] encodedChallenge)
                {
                    return null;
                }
            };
        }

        Aeron.Context context = new Aeron.Context();

        if(clientInfo.getProtocol() == ProtocolType.PROTOCOL_AERON_UDP) {
            MediaDriver.Context mediaContext = new MediaDriver.Context();
            mediaContext.dirDeleteOnStart(true).threadingMode(ThreadingMode.SHARED).dirDeleteOnShutdown(true);
            final MediaDriver driver = MediaDriver.launchEmbedded(mediaContext);
            context.aeronDirectoryName(driver.aeronDirectoryName());
        }
        else if(clientInfo.getProtocol() == ProtocolType.PROTOCOL_AERON_IPC){
            context.aeronDirectoryName(clientInfo.getShareMemoryPath());
        }

        // driver.context().aeronDirectoryName()
        ctx.aeron = Aeron.connect(context);
        ctx.conclude();
        super.start();
    }

    @Override
    protected void connect() throws ConnectionException
    {
        sendEvent(IEventListener.CONNECTION_CONNECTING);

        logger.info("aeron connect to driver ");

        String requestChannel = String.format(Consts.getAeronChannelStringTemplate(clientInfo.getProtocol()), clientInfo.getAddressHost(), clientInfo.getAddressPort());
        logger.info(String.format("publication start, request channel:[%s], request stream id:[%d]",
                requestChannel, clientInfo.getRequestStreamID()));

        AeronClientWriteService writeService = new AeronClientWriteService(messageProcessor, ctx.aeron);
        publication = ctx.aeron.addPublication(requestChannel, clientInfo.getRequestStreamID());

        String responseChannel = String.format(Consts.getAeronChannelStringTemplate(clientInfo.getProtocol()), clientInfo.getLocalHost(), clientInfo.getLocalPort());
        logger.info(String.format("subscription start, response channel:[%s], response stream id:[%d]",
                responseChannel, clientInfo.getResponseStreamID()));

        AeronClientReadService readService = new AeronClientReadService(messageProcessor, ctx.aeron, ctx);
        readService.createSubscription(responseChannel, clientInfo.getResponseStreamID());

        //simplified hand-shake process
        sendConnectSync(readService.getSubStreamChannel(), readService.getSubStreamId(), ctx.aeron.nextCorrelationId());
        AtomicBoolean syncAckReceivedWait = new AtomicBoolean(true);
        AeronUtil.subscriberLoop((buffer, offset, length, header) ->
            {
                final String msg = buffer.getStringWithoutLengthAscii(offset, length);
                logger.info(
                    String.format("Message to url %s, stream %d from session %d (%d@%d) <<%s>>%n",
                        clientInfo.getResponseChannel(), clientInfo.getResponseStreamID(), header.sessionId(), length, offset, msg));
                if (handShakeStatus == HandShakeStatus.INIT) {
                    onConnectSyncAckReceived(buffer, offset, writeService);
                    syncAckReceivedWait.set(false);
                }
            }, FRAGMENT_COUNT_LIMIT, syncAckReceivedWait).accept(readService.getSubscription());

        reader = readService;
        writer = writeService;

        sendEvent(IEventListener.CONNECTION_CONNECTED);
    }

    void onConnectSyncAckReceived(DirectBuffer buffer, int offset, AeronClientWriteService writeService)
    {
        final io.aeron.archive.codecs.MessageHeaderDecoder headerDecoder = messageHeaderDecoder;
        headerDecoder.wrap(buffer, offset);
        logger.debug("received header:" + headerDecoder.toString());

        final int schemaId = headerDecoder.schemaId();
        if (schemaId != io.aeron.archive.codecs.MessageHeaderDecoder.SCHEMA_ID) {
            throw new CedaException("expected schemaId=" + io.aeron.archive.codecs.MessageHeaderDecoder.SCHEMA_ID + ", actual=" + schemaId);
        }
        switch (messageHeaderDecoder.templateId()) {
            case AuthConnectRequestDecoder.TEMPLATE_ID: {
                final AuthConnectRequestDecoder decoder = authConnectRequest;
                decoder.wrap(buffer, offset + io.aeron.archive.codecs.MessageHeaderDecoder.ENCODED_LENGTH, headerDecoder.blockLength(), headerDecoder.version());

                logger.debug("received AuthConnectRequest:" + decoder.toString());
                writeService.createPublication(decoder.responseChannel(), decoder.responseStreamId());
                handShakeStatus = HandShakeStatus.SYNC_ACK;
                return;
            }
            default:
                throw new CedaException("unexpected handshake message");
        }
    }

    public boolean sendConnectSync(final String responseChannel, final int responseStreamId, final long correlationId)
    {
        final byte[] encodedCredentials = credentialsSupplier.encodedCredentials();
        final AuthConnectRequestEncoder connectRequestEncoder = new AuthConnectRequestEncoder();
        connectRequestEncoder
                .wrapAndApplyHeader(buffer, 0, messageHeader)
                .correlationId(correlationId)
                .responseStreamId(responseStreamId)
                .version(AeronArchive.Configuration.PROTOCOL_SEMANTIC_VERSION)
                .responseChannel(responseChannel)
                .putEncodedCredentials(encodedCredentials, 0, encodedCredentials.length);

        logger.info(String.format("send connect synchronize:", connectRequestEncoder.toString()));
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
    public void stop()
    {
    }
}

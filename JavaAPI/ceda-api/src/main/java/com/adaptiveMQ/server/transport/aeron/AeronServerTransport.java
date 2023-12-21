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
import com.adaptiveMQ.server.IServerConnectionListener;
import com.adaptiveMQ.server.IServerLoginHandler;
import com.adaptiveMQ.server.internal.ConnectionPoint;
import com.adaptiveMQ.server.internal.ConnectionPointManager;
import com.adaptiveMQ.server.transport.IChannel;
import com.adaptiveMQ.server.transport.IChannelInboundHandler;
import com.adaptiveMQ.server.transport.IServerTransport;
import com.adaptiveMQ.utils.Consts;
import io.aeron.Aeron;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.agrona.ErrorHandler;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.CountedErrorHandler;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.status.AtomicCounter;
import org.agrona.concurrent.status.StatusIndicator;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static io.aeron.archive.ArchiveThreadingMode.DEDICATED;

public class AeronServerTransport implements IServerTransport, IChannelInboundHandler
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AeronServerTransport.class);
    private final ConcurrentHashMap<IChannel, ConnectionPoint> channelMap = new ConcurrentHashMap<>();
    final AtomicBoolean running = new AtomicBoolean(true);
    static final Context transportContext = new Context();
    private ConnectionPointManager connectionPointManager;
    private IServerConnectionListener serverConnectionListener;
    private IServerLoginHandler serverConnectionAuth;
    private AeronServerConnectionAcceptor aeronServerConnectionAcceptor;
    private AgentRunner acceptorAgentRunner = null;
    private AgentRunner connectionAgentRunner = null;
    private AeronSessionWorker<AeronServerConnection> aeronSessionManager = new AeronSessionWorker<>("session-manager", AeronServerTransport.transportContext.countedErrorHandler());
    private ServiceInfo serviceInfo = null;
    private static final boolean EMBEDDED_MEDIA_DRIVER = true;

    public static final class Configuration
    {
        public static final String THREADING_MODE_PROP_NAME = "ceda.aeron.server.threading.mode";
        public static final int CEDA_ERROR_COUNT_TYPE_ID = 1011;

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

        public static AeronThreadingMode threadingMode()
        {
            return AeronThreadingMode.valueOf(System.getProperty(THREADING_MODE_PROP_NAME, DEDICATED.name()));
        }

    }

    public static class LoggingErrorHandler implements ErrorHandler, AutoCloseable
    {
        @Override
        public void close() throws Exception
        {
        }

        @Override
        public void onError(Throwable throwable)
        {
            logger.error(throwable);
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
                errorHandler = new LoggingErrorHandler();
            }
            if (null == errorCounter) {
                errorCounter = aeron.addCounter(Configuration.CEDA_ERROR_COUNT_TYPE_ID, "Ceda API Errors");
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

    // ExecutorService executorService = Executors.newSingleThreadExecutor();

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
        this.serviceInfo = serviceInfo;

        MediaDriver.Context context = new MediaDriver.Context();
        context.aeronDirectoryName(serviceInfo.getShareMemoryPath())
                    .dirDeleteOnStart(true)
                    .threadingMode(ThreadingMode.DEDICATED)
                    .dirDeleteOnShutdown(true);

        final MediaDriver driver = EMBEDDED_MEDIA_DRIVER ? MediaDriver.launchEmbedded(context) : null;

        final Aeron.Context ctx = new Aeron.Context();
        ctx.aeronDirectoryName(driver.aeronDirectoryName());
        transportContext.aeron = Aeron.connect(ctx);
        transportContext.conclude();

        logger.info(String.format("aeron transport bind:[%s]", serviceInfo.toString()));
        aeronServerConnectionAcceptor = new AeronServerConnectionAcceptor(transportContext.aeron, serviceInfo, this);

        acceptorAgentRunner = new AgentRunner(transportContext.serviceIdleStrategy(), transportContext.errorHandler(), transportContext.errorCounter(), aeronServerConnectionAcceptor);
        AgentRunner.startOnThread(acceptorAgentRunner, transportContext.threadFactory());

        connectionAgentRunner = new AgentRunner(transportContext.serviceIdleStrategy(), transportContext.errorHandler(), transportContext.errorCounter(), aeronSessionManager);
        AgentRunner.startOnThread(connectionAgentRunner, transportContext.threadFactory());
    }

    @Override
    public void exceptionCaught(IChannel channel, Throwable e)
    {
        channel.close();
    }

    @Override
    public void channelActive(IChannel channel)
    {
        String address = channel.remoteUrl();
        logger.info("connect from: " + address);

        ConnectionPoint connectionPoint = new ConnectionPoint(address, channel, serverConnectionListener, serverConnectionAuth, connectionPointManager);
        String subStreamChannel = String.format(Consts.getAeronChannelStringTemplate(serviceInfo.getProtocolType()), serviceInfo.getHost(), serviceInfo.getPort());
        logger.info(String.format("connectionPoint:[%d], subscription start:[%s]", connectionPoint.getConnectionID(), subStreamChannel));

        switch (serviceInfo.getProtocolType()) {
            case PROTOCOL_AERON_UDP:
                ((AeronServerConnection) channel).createSubscription(subStreamChannel, connectionPoint.getConnectionID());
                break;
            case PROTOCOL_AERON_IPC:
            default:
                ((AeronServerConnection) channel).createSubscription(subStreamChannel);
                break;
        }

        channelMap.put(channel, connectionPoint);
        aeronSessionManager.addSession((AeronServerConnection) channel);
    }

    @Override
    public void channelInactive(IChannel channel)
    {
        logger.info("disconnect from: " + channel.remoteUrl().toString());
        ConnectionPoint point = channelMap.get(channel);
        if (point != null) {
            channelMap.remove(channel);
            connectionPointManager.disConnect(point);
        }
    }

    @Override
    public void channelRead(IChannel channel, byte[] bytes)
    {
        ConnectionPoint point = channelMap.get(channel);
        if (point != null) {
            point.inPutData(bytes, bytes.length);
        }
    }

    @Override
    public void stop()
    {
        running.set(false);
    }
}

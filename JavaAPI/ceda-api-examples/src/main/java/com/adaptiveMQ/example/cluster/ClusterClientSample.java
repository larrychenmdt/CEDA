package com.adaptiveMQ.example.cluster;


import com.adaptiveMQ.client.ClientInfo;
import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.cluster.IClusterClient;
import com.adaptiveMQ.cluster.ServiceInfo;
import com.adaptiveMQ.example.ExampleConfiguration;
import com.adaptiveMQ.example.rpc.PingClient;
import com.adaptiveMQ.server.ServiceManager;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ClusterClientSample
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClusterClientSample.class);
    private final Set<String> m_liveConnections = new HashSet();
    private ServiceManager serviceManager = null;
    private IClusterClient clusterClient = null;

    public static void main(String[] args)
    {
        ClusterClientSample clusterClientSample = new ClusterClientSample();
        clusterClientSample.run();
    }

    private void run()
    {
        serviceManager = ServiceManager.getInstance();
        serviceManager.addRegisterEventListener((int nCode) -> {
            switch (nCode) {
                case IEventListener.CONNECTION_CONNECTED:
                    logger.info("Client: register CONNECTION_CONNECTED");
                    break;
                case IEventListener.CONNECTION_CLOSED:
                    logger.info("Client: register CONNECTION_CLOSED");
                    break;
            }
        });
        try {
            serviceManager.connectRegister(ExampleConfiguration.ZOOKEEPER_ADDRESS);
            clusterClient = serviceManager.createClustClient(ExampleConfiguration.PONG_SERVER_NAME);
            clusterClient.addListener((List<ServiceInfo> addList, List<String> removeList) -> {
                createConnection();
            });
            if(serviceManager.registerConnected()) {
                createConnection();
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private void createConnection()
    {
        List<ServiceInfo> serviceInfos = clusterClient.getWillWorkService();
        try {
            if (serviceInfos == null || serviceInfos.size() == 0) {
                logger.info("no MQServer");
            } else {
                for (ServiceInfo serviceInfo : serviceInfos) {
                    if (!m_liveConnections.contains(serviceInfo.getSequenceName())) {
                        logger.info("Server seqName=" + serviceInfo.getSequenceName());
                        PingClient pingClient = new PingClient();
                        ClientInfo clientInfo = createClientInfo(serviceInfo);
                        pingClient.start(clientInfo);
                        m_liveConnections.add(serviceInfo.getSequenceName());
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    ClientInfo createClientInfo(ServiceInfo serviceInfo)
    {
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setUser(ExampleConfiguration.LOGIN_USER_NAME, ExampleConfiguration.LOGIN_PASSWORD);
        clientInfo.setAddress(serviceInfo.getHost(), serviceInfo.getPort());
        clientInfo.setProtocol(serviceInfo.getProtocolType());
        clientInfo.setLocalHost(ExampleConfiguration.CLIENT_HOST);
        clientInfo.setLocalPort(ExampleConfiguration.CLIENT_PORT);
        clientInfo.setShareMemoryPath(serviceInfo.getShareMemoryPath());
        return clientInfo;
    }
}

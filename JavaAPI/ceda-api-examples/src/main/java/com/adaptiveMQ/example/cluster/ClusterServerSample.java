package com.adaptiveMQ.example.cluster;


import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.cluster.IClusterClient;
import com.adaptiveMQ.cluster.ServiceInfo;
import com.adaptiveMQ.example.ExampleConfiguration;
import com.adaptiveMQ.example.rpc.PongServer;
import com.adaptiveMQ.server.ServiceManager;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;


public class ClusterServerSample
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClusterServerSample.class);

    private ServiceManager serviceManager = null;
    private boolean m_isActive = false;
    private IClusterClient m_registerClient = null;

    @FunctionalInterface
    interface ClusterLeaderStartor
    {
        void start();
    }

    public static void main(String[] args) throws Exception
    {
        ClusterServerSample clusterServerSample = new ClusterServerSample();
        ServiceInfo svrInfo = PongServer.createServiceInfo();
        clusterServerSample.start(svrInfo, ExampleConfiguration.ZOOKEEPER_ADDRESS, () -> {
            PongServer pongServer = new PongServer();
            pongServer.startApp(svrInfo);
            logger.info("statService ");
        });
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (Exception e) {
            }
        }
    }

    private void start(ServiceInfo svrInfo, String zookeeperAddr, ClusterLeaderStartor clusterLeaderStartor) throws Exception
    {
        serviceManager = ServiceManager.getInstance();
        serviceManager.addRegisterEventListener((int nCode) -> {
            switch (nCode) {
                case IEventListener.CONNECTION_CONNECTED:
                    logger.info("Service: register CONNECTION_CONNECTED");
                    try {
                        m_registerClient.registService(svrInfo);
                        logger.info("register complete, seqName=" + svrInfo.getSequenceName());
                    } catch (Exception e) {
                        logger.error(e);
                    }
                    break;
                case IEventListener.CONNECTION_CLOSED:
                    logger.info("Service: register CONNECTION_CLOSED");
                    break;
            }
        });

        m_registerClient = serviceManager.createClustClient(svrInfo.getName());
        m_registerClient.addListener((List<ServiceInfo> addList, List<String> removeList) -> {
            if (!m_isActive) {
                List<ServiceInfo> infoList = m_registerClient.getWillWorkService();
                for (ServiceInfo sinfo : infoList) {
                    if (sinfo.getSequenceName().equals(svrInfo.getSequenceName())) {
                        clusterLeaderStartor.start();
                        m_isActive = true;
                    }
                }
            }
            for (String seqName : removeList) {
                logger.info("Event: removeNode=" + seqName);
            }
        });
        if (!serviceManager.registerConnected()) {
            serviceManager.connectRegister(zookeeperAddr);
        }
    }
}

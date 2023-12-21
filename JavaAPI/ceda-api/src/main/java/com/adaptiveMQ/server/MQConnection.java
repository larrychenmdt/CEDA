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

package com.adaptiveMQ.server;

import com.adaptiveMQ.client.ClientConnectionFactory;
import com.adaptiveMQ.client.ClientInfo;
import com.adaptiveMQ.client.IClientConnection;
import com.adaptiveMQ.client.IClientSession;
import com.adaptiveMQ.client.IEventListener;
import com.adaptiveMQ.client.IMessageListener;
import com.adaptiveMQ.message.BaseDestination;
import com.adaptiveMQ.message.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MQConnection implements IMQConnection
{
    private IClientConnection m_connection = null;
    private ClientInfo m_clientInfo = null;
    private IMQConnectionListener m_listener = null;
    private IMQConnectionMessageListener m_msglistener = null;
    private IClientSession m_session = null;
    private String m_signal;

    private boolean m_bLogin = false;
    private final ConcurrentHashMap<String, BaseDestination> m_HashTopic;
    private final CopyOnWriteArrayList<SubImageData> m_SubImageList;

    private final IMessageListener messageListener = (msg) -> {
        if (m_listener != null) {
            m_msglistener.onMessage(msg, this);
        }
    };

    private final  IEventListener eventListener = (int event)->
    {
        switch (event) {
            case IEventListener.CONNECTION_LOGIN_SUCCESS:
                subscribe();
                m_bLogin = true;
                break;
            case IEventListener.CONNECTION_CLOSED:
                m_bLogin = false;
                break;
            case IEventListener.CONNECTION_LOST:
                m_bLogin = false;
                break;
        }
        if (m_listener != null) {
            m_listener.onEvent(event, this);
        }
    };

    class SubImageData
    {
        List<BaseDestination> desList;
        String svrId;
        String sid;
    }

    public MQConnection()
    {
        m_SubImageList = new CopyOnWriteArrayList<SubImageData>();
        m_HashTopic = new ConcurrentHashMap<String, BaseDestination>();
    }

    public ClientInfo getClientInfo()
    {
        return m_clientInfo;
    }

    public String getSignal()
    {
        return m_signal;
    }

    public void setSignal(String sName)
    {
        m_signal = sName;
    }

    public void setConnectListener(IMQConnectionListener listener)
    {
        m_listener = listener;
    }

    public void setConnectMessageListener(IMQConnectionMessageListener listener)
    {
        m_msglistener = listener;
    }

    public void sendMessage(Message msg)
    {
        if (m_bLogin) {
            m_session.send(msg);
        }
    }

    public Message request(Message msg, long lMilliSecond)
    {
        Message rmsg = null;
        if (m_bLogin) {
            try {
                rmsg = m_session.sendRequest(msg, lMilliSecond);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return rmsg;
    }

    @Override
    public void subscribe(List<BaseDestination> destinationList)
    {
        if (m_bLogin) {
            m_session.subscribe(destinationList, messageListener);
        }
        for (BaseDestination destination : destinationList) {
            m_HashTopic.put(destination.getName(), destination);
        }
    }

    @Override
    public void subscribe(List<BaseDestination> destinationList, String svrID, String sid)
    {
        if(m_bLogin)
        {
            try {
                m_session.subscribe(destinationList, svrID, sid, messageListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SubImageData subData=new SubImageData();
        subData.desList=destinationList;
        subData.svrId=svrID;
        subData.sid=sid;
        m_SubImageList.add(subData);
    }

    @Override
    public void unSubscribe(List<BaseDestination> destinationList)
    {
        if (m_bLogin) {
            m_session.unSubscribe(destinationList, messageListener);
        }
        for (BaseDestination destination : destinationList) {
            m_HashTopic.remove(destination.getName());
        }
    }

    @Override
    public IClientSession getSession()
    {
        return m_session;
    }

    @Override
    public void connect(ClientInfo sInfo) throws Exception
    {
        m_clientInfo = sInfo;
        connect();
    }

    private void connect() throws Exception
    {
        m_connection = ClientConnectionFactory.createConnection(m_clientInfo);
        m_connection.addEventListener(eventListener);
        m_connection.setPingInterval(15);
        m_connection.start();
    }

    private void subscribe()
    {
        m_session = m_connection.createSession();

        if (m_HashTopic.size() > 0) {
            ArrayList<BaseDestination> desList = new ArrayList<BaseDestination>();
            Collection<BaseDestination> coll1 = m_HashTopic.values();
            for (BaseDestination obj : coll1) {
                desList.add(obj);

            }
            if (desList.size() > 0) {
                m_session.subscribe(desList, messageListener);
            }
            desList = null;
        }

        if (m_SubImageList.size() > 0) {
            for (SubImageData subData : m_SubImageList) {
                try {
                    m_session.subscribe(subData.desList, subData.svrId, subData.sid, messageListener);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void close()
    {
        m_bLogin = false;
        if (m_connection != null) {
            m_connection.removeEventListener(eventListener);
            m_connection.close();
            m_connection.stop();
            m_connection = null;
            m_session = null;
        }
    }

    @Override
    public boolean isLogin()
    {
        return m_bLogin;
    }
}

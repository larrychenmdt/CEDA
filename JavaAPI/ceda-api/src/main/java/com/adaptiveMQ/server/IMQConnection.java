package com.adaptiveMQ.server;

import java.util.List;
import com.adaptiveMQ.client.IClientSession;
import com.adaptiveMQ.message.Message;
import com.adaptiveMQ.message.BaseDestination;
import com.adaptiveMQ.client.ClientInfo;

public interface IMQConnection{
	
	/**
	 * 
	 * 设置连接标记，方便使用
	 * @param String sName 设置连接标记字符串
	 * @return void                       
	 * @exception 
	*/
	public void setSignal(String sName);
	
	/**
	 * 
	 * 获取连接标记，方便使用
	 * @param null
	 * @return String                       
	 * @exception 
	*/
	public String getSignal();
	
	/**
	 * 
	 * 设置连接监听器
	 * @param IMQConnectionListener listener 设置连接监听器
	 * @return void                       
	 * @exception 
	*/	
	public void setConnectListener(IMQConnectionListener listener);
	
	/**
	 * 
	 * 发送消息
	 * @param Message msg 发送的消息
	 * @return void                       
	 * @exception 
	*/
	public void sendMessage(Message msg);
	
	/**
	 * 
	 * 发送request`消息
	 * @param Message msg 发送的消息，long lMilliSecond 请求的最多时长，毫秒
	 * @return 返回的消息，或为null                       
	 * @exception 
	*/
	public Message request(Message msg,long lMilliSecond);

	/**
	 * 
	 * 连接服务器
	 * @param ClientInfo sInfo, 连接参数，地址端口和登录名称，密码
	 * @return void                       
	 * @exception  Exception
	*/
	public void connect(ClientInfo sInfo) throws Exception;
	
	/**
	 * 
	 * 断开连接
	 * @param null
	 * @return void                       
	 * @exception
	*/	
	public void close();
	
	/**
	 * 
	 * 是否登录服务器成功
	 * @param null
	 * @return  boolean 是否登录服务器成功                       
	 * @exception
	*/
	public boolean isLogin();
	
	/**
	 * 
	 * 批量订阅MQ消息
	 * @param ArrayList destinationList，topic队列
	 * @return void                       
	 * @exception
	*/
	public void subscribe(List<BaseDestination> destinationList);

	/**
	 * 
	 * 批量订阅MQ消息
	 * @param ArrayList destinationList，topic队列；String svrID,对应的服务名；String sid，用户id
	 * @return void                       
	 * @exception
	*/
	public void subscribe(List<BaseDestination> destinationList,String svrID,String sid);

	/**
	 * 
	 * 批量退订MQ消息
	 * @param ArrayList destinationList，topic队列
	 * @return void                       
	 * @exception
	*/	
	public void unSubscribe(List<BaseDestination> destinationList);

	/**
	 * 
	 * 获取连接session
	 * @param null
	 * @return IClientSession,连接对应的session                       
	 * @exception
	*/
	public IClientSession getSession();
}

package com.adaptiveMQ.server;

import com.adaptiveMQ.message.Message;

public interface IMQConnectionMessageListener
{
	/**
	 * 
	 * 响应消息
	 * @param Message msg  接收到的消息,IMQConnection connHandler 对应连接
	 * @return void                       
	 * @exception 
	*/
	void onMessage(Message msg, IMQConnection connHandler);
}

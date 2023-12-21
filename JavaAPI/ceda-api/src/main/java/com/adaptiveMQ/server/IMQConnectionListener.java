package com.adaptiveMQ.server;

public interface IMQConnectionListener {

	/**
	 *
	 * 响应连接事件
	 * @param int event  事件代号,IMQConnection connHandler 对应连接
	 * @return void
	 * @exception
	*/
	void onEvent(int event, IMQConnection connHandler);

}

package com.adaptiveMQ.message.internal;

public class MsgSerializerFactory {
	public static IMsgSerializer Create(MsgSerializerType type) {
		IMsgSerializer ms = null;
		switch (type) {
		case Json:
			ms = new JsonSerializer();
			break;
		default:
			ms = new JsonSerializer();
			break;
		}
		return ms;
	}
}

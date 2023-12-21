package com.adaptiveMQ.message.internal;

import com.adaptiveMQ.message.SerializerException;

import java.util.List;

public class MsgSerializerHelper
{
	public static <T> String Serializer(T t, MsgSerializerType type)
			throws SerializerException
	{
		String str = "";
		IMsgSerializer ms = MsgSerializerFactory.Create(type);
		str = ms.Serializer(t);
		return str;
	}

	public static <T> T Deserialize(String msg, Class<T> clazz,
			MsgSerializerType type) throws SerializerException {
		T t = null;
		IMsgSerializer ms = MsgSerializerFactory.Create(type);
		t = ms.Deserialize(msg, clazz);
		return t;
	}

	public static <T> List<T> Deserialize2(String msg, Class<T> clazz,
			MsgSerializerType type) throws SerializerException {
		List<T> t = null;
		IMsgSerializer ms = MsgSerializerFactory.Create(type);
		t = ms.Deserialize2(msg, clazz);
		return t;
	}

	public static <T> T Clone(T t, Class<T> clazz, MsgSerializerType type)
			throws SerializerException {
		T t2 = null;
		IMsgSerializer ms = MsgSerializerFactory.Create(type);
		t2 = ms.Clone(t, clazz);
		return t2;
	}

	 
}

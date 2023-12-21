package com.adaptiveMQ.message.internal;

import com.adaptiveMQ.message.BaseMsgSerializer;
import com.alibaba.fastjson.JSON;

import java.util.List;

public class JsonSerializer extends BaseMsgSerializer
{

	@Override
	public <T> String Serializer(T t) throws JsonException {
		String str = "";
		try {
			str = JSON.toJSONString(t);
		} catch (Exception ex) {
			throw new JsonException("Serializer Failed", ex);
		}
		return str;
	}

	@Override
	public <T> T Deserialize(String msg, Class<T> clazz) throws JsonException {
		T t = null;
		try {
			t = JSON.parseObject(msg, clazz);
		} catch (Exception ex) {
			throw new JsonException("Deserialize Failed", ex);
		}
		return t;
	}

	@Override
	public <T> List<T> Deserialize2(String msg, Class<T> clazz)
			throws JsonException {
		List<T> t = null;
		try {

			t = JSON.parseArray(msg, clazz);
		} catch (Exception ex) {
			throw new JsonException("Deserialize Failed", ex);
		}
		return t;
	}

	@Override
	public <T> T Clone(T t, Class<T> clazz) throws JsonException {
		T t2 = null;
		try {
			String str = Serializer(t);
			t2 = Deserialize(str, clazz);
		} catch (Exception ex) {
			throw new JsonException("Clone Failed", ex);
		}
		return t2;
	}

	 
	
}

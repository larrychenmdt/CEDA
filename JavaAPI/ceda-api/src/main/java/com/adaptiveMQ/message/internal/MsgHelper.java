package com.adaptiveMQ.message.internal;

import com.adaptiveMQ.message.SerializerException;

import java.util.List;

public class MsgHelper {

	private static MsgSerializerType defaultMsgSerializerType = Consts.defaultMsgSerializerType;

	public static <T> String Serializer(T t, MsgSerializerType type,
			CharsetConvertType CharsetConvertType) throws SerializerException
    {
		String msg = MsgSerializerHelper.Serializer(t, type);

		msg = CharsetConvert(msg, CharsetConvertType);
		return msg;
	}

	public static <T> String Serializer(T t,
			CharsetConvertType CharsetConvertType) throws SerializerException {
		String msg = MsgSerializerHelper
				.Serializer(t, defaultMsgSerializerType);

		msg = CharsetConvert(msg, CharsetConvertType);
		return msg;
	}

	public static <T> T Deserialize(String msg, Class<T> clazz,
			MsgSerializerType type, CharsetConvertType CharsetConvertType)
			throws SerializerException {

		msg = CharsetConvert(msg, CharsetConvertType);
		return MsgSerializerHelper.Deserialize(msg, clazz, type);
	}

	public static <T> T Deserialize(String msg, Class<T> clazz,
			CharsetConvertType CharsetConvertType) throws SerializerException {

		return Deserialize(msg, clazz, defaultMsgSerializerType,
				CharsetConvertType);
	}

	public static <T> List<T> Deserialize2(String msg, Class<T> clazz,
			MsgSerializerType type, CharsetConvertType CharsetConvertType)
			throws SerializerException {

		msg = CharsetConvert(msg, CharsetConvertType);
		return MsgSerializerHelper.Deserialize2(msg, clazz, type);
	}

	public static <T> List<T> Deserialize2(String msg, Class<T> clazz,
			CharsetConvertType CharsetConvertType) throws SerializerException {

		return Deserialize2(msg, clazz, defaultMsgSerializerType,
				CharsetConvertType);
	}

	public static <T> T Clone(T t, Class<T> clazz, MsgSerializerType type)
			throws SerializerException {

		return MsgSerializerHelper.Clone(t, clazz, type);
	}

	public static <T> T Clone(T t, Class<T> clazz) throws SerializerException {
		return Clone(t, clazz, defaultMsgSerializerType);
	}

	public static String CharsetConvert(String msg,
			CharsetConvertType CharsetConvertType) {

		switch (CharsetConvertType) {
		case GBKToISO8859_1:
			msg = CharsetConvert.charsetConvert(msg);
			break;
		case ISO8859_1ToGBK:
			msg = CharsetConvert.ISO_8859_1ToGBK(msg);
			break;
		}
		return msg;
	}

	 
}

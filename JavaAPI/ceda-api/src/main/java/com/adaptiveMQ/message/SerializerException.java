package com.adaptiveMQ.message;

public class SerializerException extends Exception {

	private static final long serialVersionUID = 1L;

	public SerializerException(String message) {
		super(message);
	}

	public SerializerException(String message, Throwable cause) {
		super(message, cause);
	}

	public SerializerException(Throwable cause) {
		super(cause);
	}
}

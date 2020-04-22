package com.platform.website.common;
public class MyException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -321956790185436065L;
  
	public MyException(String message) {
		super(message,new Throwable(message));
	}

	public MyException(Throwable cause) {
		super(cause);
	}

	public MyException(String message, Throwable cause) {
		super(message, cause);
	}
	
}


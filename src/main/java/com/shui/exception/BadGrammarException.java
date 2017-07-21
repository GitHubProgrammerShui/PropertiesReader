package com.shui.exception;

public class BadGrammarException extends RuntimeException{
	/**
	 * 
	 */
	private static final long serialVersionUID = 546678995042779556L;
	
	private String message;
	
	public BadGrammarException(String message) {
		this.message=message;
	}
	
	@Override
	public String toString() {
		System.out.println("######"+message+"######");
		return super.toString();
	}
}

package com.shui.exception;

public class BadGrammarException extends RuntimeException{
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

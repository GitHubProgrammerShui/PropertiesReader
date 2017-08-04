package com.shui.exception;

public class PropertiesBuildException extends RuntimeException{

	private static final long serialVersionUID = 1L;
	
	private String message;
	
	public PropertiesBuildException(){
	}
	public PropertiesBuildException(String message){
		this.message=message;
	}
	public PropertiesBuildException(String message, Throwable cause) {
		super(message, cause);
		this.message=message;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString(){
		String s = getClass().getName();
        return (message != null) ? (s + ": " + message) : s;
	}
}

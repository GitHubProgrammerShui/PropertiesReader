package com.shui.model;

import com.shui.constant.PropertyValueType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("rawtypes")
public class Property{
	private String key;
	private Class objectClass;
	private boolean stringValue;
	private Object value;
	
	@SuppressWarnings("unchecked")
	public <T> T getOriginalValue(Class<T> valueClass){
		if(valueClass!=null&&valueClass.equals(value.getClass())){
			return (T) value;
		}else{
			return null;
		}
	}
}

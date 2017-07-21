package com.test;

import java.io.IOException;

import org.jdom.JDOMException;
import org.junit.Test;

import com.shui.reader.PropertiesReader;

public class TestPropertiesReader {
	
	@Test
	public void testProperties() throws JDOMException, IOException{
		PropertiesReader reader=new PropertiesReader();
		reader.load("classpath:properties/reader/PropertiesReader.xml");
		System.out.println(reader.getStringValue("innerKeyOfProp2"));
	}
}

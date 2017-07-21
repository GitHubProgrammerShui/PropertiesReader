package com.shui.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.shui.exception.BadGrammarException;
import com.shui.model.Property;

public class PropertiesReader {
	
	private List<Property> list=new ArrayList<>();
	
	/**
	 * 从某个输入流中加载xml文件
	 * @param xmlInputStream xml文件输入流
	 * @throws JDOMException
	 * @throws IOException
	 */
	public void load(InputStream xmlInputStream) throws JDOMException, IOException{
		Property property=null;
		List<Element> eleList=null;
		
		//加载xml文件并创建出对象
		SAXBuilder saxBuilder=new SAXBuilder();
		Document document=saxBuilder.build(xmlInputStream);
		Element root=document.getRootElement();
		//搜索该文件下配置的属性信息并分析
		List<Element> properties=root.getChildren();
		for (Element e: properties){
			if("import".equals(e.getName())){
				String location=null;
				if(e.getAttribute("location")!=null){
					location=e.getAttributeValue("location");
					this.load(location);
				}
			}else if("property".equals(e.getName())){
				property=new Property();
				property.setKey(e.getAttributeValue("key"));
				eleList=e.getChildren();
				if(eleList!=null&&eleList.size()<=1){
					e=eleList.get(0);
					if("list".equals(e.getName())){
						property.setType(List.class);
						property.setValue(this.processListProperty(e));
					}else if("map".equals(e.getName())){
						property.setType(Map.class);
						property.setValue(this.processMapProperty(e));
					}else if("string".equals(e.getName())){
						property.setType(String.class);
						property.setValue(this.processStringProperty(e));
					}
					list.add(property);
				}else{
					throw new BadGrammarException("一个properties中只能有一个元素(list，map或string)");
				}
			}
		}
	}
	
	public void load(String propertiesFileLocation) throws JDOMException, IOException{
		if(propertiesFileLocation!=null&&propertiesFileLocation.startsWith("classpath:")){
			String classpath=propertiesFileLocation.substring(10);
			this.load(this.getClass().getClassLoader().getResourceAsStream(classpath));
		}else{
			this.load(new FileInputStream(propertiesFileLocation));
		}
	}
	
	private List<String> processListProperty(Element listElement){
		List<Element> stringElementList=listElement.getChildren();
		List<String> result=new ArrayList<>();
		for(Element element:stringElementList){
			result.add(element.getText());
		}
		return result;
	}
	private String processStringProperty(Element stringElement){
		return stringElement.getText();
	}
	private Map<String,String> processMapProperty(Element mapElement){
		List<Element> mapElementList=mapElement.getChildren();
		Map<String, String> result=new HashMap<>();
		for(Element entry:mapElementList){
			result.put(entry.getAttributeValue("key"), entry.getText());
		}
		return result;
	}
	
	/**
	 * 如果要获取的属性值实际上是一个字符串，就像下面这样：<br>
	 * &lt;property&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;string&gt;hello world&lt;/string&gt;<br>
	 * &lt;/property&gt;<br>
	 * 那么可以使用这个方法直接获取该字符串值
	 * @param key 属性key
	 * @return 该属性对应的属性值
	 */
	public String getStringValue(String key){
		for (Property property : list) {
			if(key!=null&&key.equals(property.getKey())){
				if(property.getType().getName().equals("java.lang.String")){
					return (String) property.getValue();
				}
			}
		}
		return null;
	}
	
	/**
	 * 如果要获取的属性值实际上是一个列表，就像下面这样：<br>
	 * &lt;property&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;list&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;string&gt;hello&lt/string&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;string&gt;world&lt/string&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/list&gt;<br>
	 * &lt;/property&gt;<br>
	 * 那么可以使用该方法以列表的形式返回该属性值
	 * @param key
	 * @return
	 */
	public List<String> getListValue(String key){
		for (Property property : list) {
			if(key!=null&&key.equals(property.getKey())){
				if(property.getValue() instanceof List){
					return (List<String>) property.getValue();
				}
			}
		}
		return null;
	}
	
	/**
	 * 如果要获取的属性值实际上是一个Map，就像下面这样：<br>
	 * &lt;property&gt<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;map&gt<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;entry&nbsp;key=&quot;word1&quot;&gt;hello&lt;/entry&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;entry&nbsp;key=&quot;word2&quot;&gt;world&lt;/entry&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/map&gt<br>
	 * &lt;/property&gt<br>
	 * 那么可以使用下面的函数以map形式获取该属性值
	 * @param key
	 * @return
	 */
	public Map<String, String> getMapValue(String key){
		for (Property property : list) {
			if(key!=null&&key.equals(property.getKey())){
				if(property.getValue() instanceof Map){
					return (Map<String, String>) property.getValue();
				}
			}
		}
		return null;
	}
	/**
	 * 获取一个属性的属性值
	 * @param key 属性key
	 * @return 属性值
	 */
	public Object getValue(String key){
		for (Property property : list) {
			if(key!=null&&key.equals(property.getKey())){
				return property.getValue();
			}
		}
		return null;
	}
	
	/**
	 * 获取一个属性的类型
	 * @param key 属性key
	 * @return 属性类型，可以是String，List，Map类型，以Class形式返回
	 */
	public Class getType(String key){
		for (Property property : list) {
			if(key!=null&&key.equals(property.getKey())){
				return property.getType();
			}
		}
		return null;
	}
}

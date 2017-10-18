package com.shui.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import com.shui.constant.PropertyValueType;
import com.shui.exception.PropertiesBuildException;
import com.shui.model.Property;
import static com.shui.constant.ElementAndAttributeDefine.ATTR_ENTRY_KEY;
import static com.shui.constant.ElementAndAttributeDefine.ATTR_IMPORT_LOCATION;
import static com.shui.constant.ElementAndAttributeDefine.ATTR_PROPERTY_KEY;
import static com.shui.constant.ElementAndAttributeDefine.ELEMENT_ENTRY;
import static com.shui.constant.ElementAndAttributeDefine.ELEMENT_IMPORT;
import static com.shui.constant.ElementAndAttributeDefine.ELEMENT_LIST;
import static com.shui.constant.ElementAndAttributeDefine.ELEMENT_MAP;
import static com.shui.constant.ElementAndAttributeDefine.ELEMENT_PROPERTY;
import static com.shui.constant.ElementAndAttributeDefine.ELEMENT_STRING;

public class PropertiesReader{
	
	private List<Property> properties;
	private boolean cachable=true;
	private InputStream propertiesFileStream;
	private Namespace namespace;
	
	public PropertiesReader(){}
	
	public PropertiesReader(File file){
		try {
			this.load(new FileInputStream(file));
		}catch(FileNotFoundException e){
			throw new PropertiesBuildException("找不到指定的文档",e);
		}
	}
	public PropertiesReader(String location){
		this.load(location);
	}
	public PropertiesReader(InputStream inputStream){
		this.load(inputStream);
	}
	
	public void load(String location){
		this.load(loadResource(location));
	}
	
	public void load(InputStream inputStream){
		try {
			this.propertiesFileStream=inputStream;
			properties=this.loadToList(inputStream);
		} catch (JDOMException | IOException e) {
			throw new PropertiesBuildException("解析异常",e);
		}
	}
	
	private InputStream loadResource(String location){
		InputStream resourceStream=null;
		if(location.startsWith("classpath:")){
			location=location.substring(10);
			resourceStream=this.getClass().getClassLoader().getResourceAsStream(location);
		}else if(location.startsWith("classpath*:")){
			location=location.substring(11);
			resourceStream=ClassLoader.getSystemResourceAsStream(location);
		}else{
			try {
				resourceStream=new FileInputStream(location);
			} catch (FileNotFoundException e) {
				throw new PropertiesBuildException("找不到指定的路径",e);
			}
		}
		if(resourceStream==null){
			throw new PropertiesBuildException("找不到指定的路径");
		}
		return resourceStream;
	}
	
	private void setNamespace(Namespace namespace){
		this.namespace=namespace;
	}
	
	@SuppressWarnings("unchecked")
	private List<Property> loadToList(InputStream inputStream) throws JDOMException, IOException{
		Property property=null;
		List<Property> resultList=new ArrayList<>();
		SAXBuilder builder=new SAXBuilder(false);
		Document document=builder.build(inputStream);
		if(document.hasRootElement()){
			Element rootElement=document.getRootElement();
			this.setNamespace(rootElement.getNamespace());
			List<Element> importAndProperty=rootElement.getChildren();
			Element tempElement=null;
			Element tempEntryElement=null;
			for(Element element:importAndProperty){
				if(ELEMENT_IMPORT.equals(element.getName())){
					if(StringUtils.isNotBlank(element.getAttributeValue(ATTR_IMPORT_LOCATION))){
						resultList.addAll(this.importProperties(element.getAttributeValue(ATTR_IMPORT_LOCATION)));
					}else{
						throw new PropertiesBuildException("import元素中没有location属性或值为空白串或空串");
					}
				}else if(ELEMENT_PROPERTY.equals(element.getName())){
					//根据property元素中值的类型进行相应的处理，处理完成后对结果进行收集
					if(StringUtils.isBlank(element.getAttributeValue(ATTR_PROPERTY_KEY))){
						throw new PropertiesBuildException("property元素没有key值");
					}
					property=new Property();
					property.setKey(element.getAttributeValue(ATTR_PROPERTY_KEY));
					if((tempElement=element.getChild(ELEMENT_STRING,namespace))!=null){
						property.setObjectClass(String.class);
						property.setStringValue(true);
						property.setValue(this.extractStringValue(tempElement));
					}else if((tempElement=element.getChild(ELEMENT_LIST,namespace))!=null){
						property.setObjectClass(List.class);
						property.setStringValue(tempElement.getChild(ELEMENT_STRING, namespace)!=null);
						property.setValue(this.extractListValue(tempElement));
					}else if((tempElement=element.getChild(ELEMENT_MAP,namespace))!=null){
						property.setObjectClass(Map.class);
						
						tempEntryElement=tempElement.getChild(ELEMENT_ENTRY,namespace);
						if(tempEntryElement!=null) {
							property.setStringValue(tempEntryElement.getChild(ELEMENT_STRING, namespace)!=null);
						}
						
						property.setValue(this.extractMapValue(tempElement));
					}else{
						property.setObjectClass(null);
						property.setStringValue(false);
						property.setValue(null);
					}
					resultList.add(property);
				}
			}
		}else{
			throw new PropertiesBuildException("没有根元素");
		}
		return resultList;
	}
	private List<Property> importProperties(String location) throws JDOMException, IOException{
		InputStream inputStream=loadResource(location);
		return loadToList(inputStream);
	}
	private String extractStringValue(Element propertyValueElement){
		return propertyValueElement.getText();
	}
	@SuppressWarnings("unchecked")
	private List<Object> extractListValue(Element propertyValueElement){
		List<Object> resultList=new ArrayList<>();
		List<Element> propertyElementList=propertyValueElement.getChildren();
		for(Element element:propertyElementList){
			if(ELEMENT_STRING.equals(element.getName())){
				resultList.add(this.extractStringValue(element));
			}else if(ELEMENT_LIST.equals(element.getName())){
				resultList.add(this.extractListValue(element));
			}else if(ELEMENT_MAP.equals(element.getName())){
				resultList.add(this.extractMapValue(element));
			}
		}
		return resultList;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String,Object> extractMapValue(Element propertyValueElement){
		Map<String,Object> map=new HashMap<>();
		String entryKey=null;
		Element entryValue=null;
		List<Element> entryValueList=null;
		List<Element> propertyElementList=propertyValueElement.getChildren();
		for(Element element:propertyElementList){
			if(ELEMENT_ENTRY.equals(element.getName())){
				entryKey=element.getAttributeValue(ATTR_ENTRY_KEY);
				if(StringUtils.isBlank(entryKey)){
					throw new PropertiesBuildException("entry标签必须拥有key属性，且其值不能为空串或空白串");
				}else if(map.containsKey(entryKey)){
					throw new PropertiesBuildException("一个map下的每一个entry只能拥有唯一的key值");
				}else{
					entryValueList=element.getChildren();
					if(entryValueList!=null&&!entryValueList.isEmpty()){
						entryValue=entryValueList.get(0);
						if(ELEMENT_STRING.equals(entryValue.getName())){
							map.put(entryKey, this.extractStringValue(entryValue));
						}else if(ELEMENT_LIST.equals(entryValue.getName())){
							map.put(entryKey, this.extractListValue(entryValue));
						}else if(ELEMENT_MAP.equals(entryValue.getName())){
							map.put(entryKey, this.extractMapValue(entryValue));
						}
					}
				}
			}else{
				throw new PropertiesBuildException("map中只能包含entry标签");
			}
		}
		return map;
	}
	
	/**
	 * 获取属性对象
	 * @param key 属性key
	 * @return
	 */
	private Property getProperty(String key) {
		if(!cachable) {
			this.load(propertiesFileStream);
		}
		for(Property property:properties) {
			if(StringUtils.equals(key, property.getKey())) {
				return property;
			}
		}
		return null;
	}
	
	/**
	 * 获取值，以Object的形式返回
	 * @param key 属性键
	 * @return 属性值，可能是String，List和Map。
	 */
	public Object getObject(String key){
		if(!cachable){
			this.load(propertiesFileStream);
		}
		for(Property property:properties){
			if(StringUtils.equals(key, property.getKey())){
				return property.getValue();
			}
		}
		return null;
	}
	
	/**
	 * 如果要获取的值是一种string类型，则可以用该方法以string的形式获取该值
	 * @param key
	 * @return
	 */
	public String getString(String key){
		Object value=this.getObject(key);
		if(value instanceof String){
			return (String) value;
		}else{
			return null;
		}
	}
	/**
	 * 如果要获取的值是一种list类型，则可以用该方法以list的形式获取该值
	 * @param key
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Object> getList(String key){
		Object value=this.getObject(key);
		if(value instanceof List){
			return (List<Object>) value;
		}else{
			return null;
		}
	}
	
	/**
	 * 当一个list类型的property中list的元素都是string时，可以使用该方法直接以string泛型获取
	 * @param key
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getStringList(String key){
		Property property=this.getProperty(key);
		if(property==null) {
			return null;
		}
		if(property.getValue() instanceof List) {
			if(property.isStringValue()) {
				return (List<String>) (property.getValue());
			}
		}
		return null;
	}
	
	/**
	 * 如果要获取的值位于嵌套的list标签中，可以采用这个方法<br>
	 * 在配置文件中，由于list中可以嵌套list，因此如果希望访问嵌套的list中某一个索引处的
	 * 值需要遍历，该方法省却了使用者需要遍历获取值的麻烦，将遍历的过程直接封装为一个方法
	 * ，例如，假定属性配置文件中有如下配置：<br>
	 * &lt;property key="innerlist"&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;list&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;list&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;string&gt;string00&lt;/string&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;string&gt;string01&lt;/string&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/list&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/list&gt;<br>
	 * &lt;/property&gt;<br>
	 * 则可以像下面这样获取值：<br>
	 * getListValue("innerlist",0,0);		//string00<br>
	 * getListValue("innerlist",0,1);		//string01<br>
	 * @param key 属性键
	 * @param indexes 从外到内表示的索引
	 * @return 相应的值，如果获取到的不是字符串则返回null
	 */
	@SuppressWarnings("unchecked")
	public String getListValue(String key,int... indexes){
		Object value=this.getObject(key);
		List<Object> list=null;
		int i=0;
		do{
			if(value==null){
				return null;
			}else if(value instanceof List){
				list=(List<Object>) value;
				value=list.get(indexes[i]);
				i++;
			}else{
				return null;
			}
		}while(i<indexes.length);
		if(value!=null&&value instanceof String){
			return (String) value;
		}else{
			return null;
		}
	}
	/**
	 * 如果要获取的是一个Map类型，则可以使用该方法
	 * @param key 属性键
	 * @return 以Map类型返回相应的值，如果实际属性值类型不是Map型则返回null
	 */
	@SuppressWarnings("unchecked")
	public Map<String,Object> getMap(String key){
		Object value=this.getObject(key);
		if(value instanceof Map){
			return (Map<String, Object>) value;
		}else{
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, String> getStringMap(String key){
		Property prop=this.getProperty(key);
		if(prop==null) {
			return null;
		}else {
			if(prop.getValue() instanceof Map) {
				if(prop.isStringValue()) {
					return (Map<String, String>) prop.getValue();
				}
			}
			return null;
		}
	}
	
	/**
	 * 如果要获取的值位于嵌套的map中，则可以通过该方法，按顺序给定相应的key，
	 * PropertiesReader会按顺序一层一层向内搜索，直到搜索到确定的String类型值，
	 * 若搜索到中途发现类型不匹配或搜索到最后的结果不为String类型则返回null
	 * @param key 属性键
	 * @param entryKeys 从外到内表示的entry键
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String getMapValue(String key,String... entryKeys){
		Object value=this.getObject(key);
		Map<String,Object> map=null;
		int index=0;
		do{
			if(value==null){
				return null;
			}else if(value instanceof Map){
				map=(Map<String,Object>) value;
				value=map.get(entryKeys[index]);
				index++;
			}else{
				return null;
			}
		}while(index<entryKeys.length);
		if(value!=null&&value instanceof String){
			return (String) value;
		}else{
			return null;
		}
	}
	
	/**
	 * 该方法相当于getListValue和getMapValue两个方法的结合版，如果
	 * 一个属性中list和map混合嵌套，则可以使用这个方法，具体使用方式和getListValue
	 * 与getMapValue的使用方法类似。
	 * @param key 属性键
	 * @param indexOrEntryKey 索引或entry键，PropertiesReader会按照给定的索引或entry键一层
	 * 一层向内搜索，直到搜索到符合条件的值，如果在搜索过程中发现类型不匹配或搜索到最后的值
	 * 不是String类型，则返回null
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String getCollectionValue(String key,Object... indexOrEntryKey){
		Object value=this.getObject(key);
		List<Object> list=null;
		Map<String,Object> map=null;
		int index=0;
		String entryKey=null;
		int i=0;
		do{
			if(value==null){
				return null;
			}else if(value instanceof Map){
				if(indexOrEntryKey[i]!=null&&indexOrEntryKey[i] instanceof String){
					entryKey=(String) indexOrEntryKey[i];
					map=(Map<String, Object>) value;
					value=map.get(entryKey);
					i++;
				}else{
					return null;
				}
			}else if(value instanceof List){
				if(indexOrEntryKey[i]!=null&&indexOrEntryKey[i] instanceof Integer){
					index=(int) indexOrEntryKey[i];
					list=(List<Object>) value;
					value=list.get(index);
					i++;
				}else{
					return null;
				}
			}else{
				return null;
			}
		}while(i<indexOrEntryKey.length);
		if(value!=null&&value instanceof String){
			return (String) value;
		}else{
			return null;
		}
	}
	
	/**
	 * 检查配置文件中是否存在某个key
	 * @param key
	 * @return
	 */
	public boolean containsKey(String key){
		if(!cachable){
			this.load(propertiesFileStream);
		}
		for(Property property:properties){
			if(StringUtils.equals(key, property.getKey())){
				return true;
			}
		}
		return false;
	}
}


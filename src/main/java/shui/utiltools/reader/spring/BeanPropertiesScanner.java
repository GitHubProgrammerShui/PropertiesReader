package shui.utiltools.reader.spring;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.shui.exception.PropertiesInjectException;
import com.shui.reader.PropertiesReader;

import shui.utiltools.reader.spring.annotation.Property;

public class BeanPropertiesScanner implements ApplicationContextAware,BeanDefinitionRegistryPostProcessor{

	private ApplicationContext context;

	private String basePackage;
	private PropertiesReader propertiesReader;

	private boolean isNotInjected=true;

	public String getBasePackage(){
		return basePackage;
	}

	public void setBasePackage(String basePackage){
		this.basePackage=basePackage;
	}

	public PropertiesReader getPropertiesReader(){
		return propertiesReader;
	}

	public void setPropertiesReader(PropertiesReader propertiesReader){
		this.propertiesReader=propertiesReader;
	}

	/*
	 * 下面两个方法配合使用，扫描指定包下所有的类，包括子包中的类也一并扫描
	 */
	private List<Class<?>> scanClasses() throws URISyntaxException,ClassNotFoundException{
		List<Class<?>> classList=new ArrayList<>();
		ClassLoader classLoader=ClassLoader.getSystemClassLoader();
		String basePackagePath=StringUtils.replaceChars(basePackage,".",File.separator);
		File basePackageFile=FileUtils.toFile(ClassLoader.getSystemResource(basePackagePath));
		if(basePackageFile!=null&&basePackageFile.isDirectory()){
			Collection<File> classFileList=FileUtils.listFiles(basePackageFile,new String[]{"class"},true);
			for(File classFile:classFileList){
				Class<?> loadClass=classLoader.loadClass(basePackage+this.getClassName(basePackageFile,classFile));
				classList.add(loadClass);
			}
		}
		return classList;
	}

	private String getClassName(File base,File classFile){
		String packagePath=StringUtils.removeStart(classFile.getAbsolutePath(),base.getAbsolutePath());
		packagePath=StringUtils.removeEnd(packagePath,".class");
		return StringUtils.replaceChars(packagePath,File.separatorChar,'.');
	}

	@SuppressWarnings("all")
	public void inject() throws IllegalArgumentException,IllegalAccessException{
		List<Class<?>> classList;
		try{
			classList=this.scanClasses();
		}catch(ClassNotFoundException|URISyntaxException e){
			throw new PropertiesInjectException("找不到指定的类",e);
		}

		// 定义相关参数
		Field[] fields=null;
		Map<String,?> beanMap=null;
		Property property=null;
		boolean isAccessible;
		String key;

		for(Class<?> cls:classList){
			fields=cls.getDeclaredFields();
			for(Field field:fields){
				if((property=field.getAnnotation(Property.class))!=null){
					key=property.key();
					beanMap=context.getBeansOfType(cls);
					isAccessible=field.isAccessible();
					field.setAccessible(true);
					for(Object obj:beanMap.values()){
						field.set(obj,propertiesReader.getObject(key));
					}
					field.setAccessible(isAccessible);
				}
			}
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException{
		this.context=applicationContext;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException{
		System.out.println("处理");
		if(isNotInjected){
			try{
				this.inject();
				isNotInjected=false;
			}catch(IllegalArgumentException|IllegalAccessException e){
				throw new RuntimeException("注入异常",e);
			}
		}
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException{
		System.out.println("处理");
		if(isNotInjected){
			try{
				this.inject();
				isNotInjected=false;
			}catch(IllegalArgumentException|IllegalAccessException e){
				throw new RuntimeException("注入异常",e);
			}
		}
	}
}

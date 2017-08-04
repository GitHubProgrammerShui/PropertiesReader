package shui.utiltools.reader.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

import com.shui.reader.PropertiesReader;

public class SpringPropertiesReaderBean extends PropertiesReader implements BeanDefinitionRegistryPostProcessor{
	
	private boolean isNotLoaded=true;
	private String location;

	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException{
		if(isNotLoaded){
			this.load(location);
		}
	}
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException{
		if(isNotLoaded){
			this.load(location);
		}
	}
}

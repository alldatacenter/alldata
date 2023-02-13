package com.alibaba.tesla.gateway.server.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.stereotype.Component;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Slf4j
@Component
public class TeslaBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory)
        throws BeansException {
        //即将spring cloud gateway 的这个bean移除掉
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
        if (registry.containsBeanDefinition("routeDefinitionRouteLocator")){
            registry.removeBeanDefinition("routeDefinitionRouteLocator");
            log.info("remove route routeDefinitionRouteLocator");
        }
        if(registry.containsBeanDefinition("filteringWebHandler")){
            registry.removeBeanDefinition("filteringWebHandler");
            log.info("remove filteringWebHandler");
        }
        if(registry.containsBeanDefinition("routePredicateHandlerMapping")){
            registry.removeBeanDefinition("routePredicateHandlerMapping");
            log.info("remove routePredicateHandlerMapping");
        }

    }
}

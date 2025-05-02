package com.platform.rpc.remoting.provider.impl;

import com.platform.rpc.remoting.provider.XxlRpcProviderFactory;
import com.platform.rpc.remoting.provider.annotation.XxlRpcService;
import com.platform.rpc.util.XxlRpcException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

public class XxlRpcSpringProviderFactory extends XxlRpcProviderFactory
		implements ApplicationContextAware, InitializingBean,DisposableBean {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(XxlRpcService.class);
        if (serviceBeanMap!=null && serviceBeanMap.size()>0) {
            for (Object serviceBean : serviceBeanMap.values()) {
                // valid
                if (serviceBean.getClass().getInterfaces().length ==0) {
                    throw new XxlRpcException("xxl-rpc, service(XxlRpcService) must inherit interface.");
                }
                // add service
                XxlRpcService xxlRpcService = serviceBean.getClass().getAnnotation(XxlRpcService.class);

                String iface = serviceBean.getClass().getInterfaces()[0].getName();
                String version = xxlRpcService.version();

                super.addService(iface, version, serviceBean);
            }
        }

        // TODO，addServices by api + prop

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.start();
    }

    @Override
    public void destroy() throws Exception {
        super.stop();
    }

}

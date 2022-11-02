package com.alibaba.tesla.authproxy.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

/**
 * Spring 上下文直接访问工具
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Configuration
public class ApplicationContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext = null;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * 获取当前的 application context。注意：必须在 Spring 已经启动完毕后调用，否则会返回 null
     *
     * @return Spring Application Context
     */
    public static ApplicationContext getContext() {
        return applicationContext;
    }
}

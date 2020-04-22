package com.platform.website.utils;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ApplicationContextUtil {
    private static ApplicationContext context;
 
    static {
        context = new ClassPathXmlApplicationContext("spring.xml", "spring-mybatis.xml");
    }
 
    public static ApplicationContext getApplicationContext() {
        return context;
    }
}
package com.alibaba.tesla.appmanager.spring.util;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * Spring Bean 工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class SpringBeanUtil {

    private static ApplicationContext context;

    public static void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        if (context == null) {
            throw new AppException(AppErrorCode.UNKNOWN_ERROR, "application context not ready");
        }
        return context;
    }

    public static <T> T getBean(Class<T> type) {
        return context.getBean(type);
    }
}

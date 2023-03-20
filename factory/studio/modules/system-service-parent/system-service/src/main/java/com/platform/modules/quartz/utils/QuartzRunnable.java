
package com.platform.modules.quartz.utils;

import lombok.extern.slf4j.Slf4j;
import com.platform.utils.SpringContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ReflectionUtils;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * 执行定时任务
 * @author AllDataDC
 */
@Slf4j
public class QuartzRunnable implements Callable<Object> {

	private final Object target;
	private final Method method;
	private final String params;

	QuartzRunnable(String beanName, String methodName, String params)
			throws NoSuchMethodException, SecurityException {
		this.target = SpringContextHolder.getBean(beanName);
		this.params = params;
		if (StringUtils.isNotBlank(params)) {
			this.method = target.getClass().getDeclaredMethod(methodName, String.class);
		} else {
			this.method = target.getClass().getDeclaredMethod(methodName);
		}
	}

	@Override
	@SuppressWarnings("all")
	public Object call() throws Exception {
		ReflectionUtils.makeAccessible(method);
		if (StringUtils.isNotBlank(params)) {
			method.invoke(target, params);
		} else {
			method.invoke(target);
		}
		return null;
	}
}

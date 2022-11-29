package cn.datax.service.quartz.quartz;

import java.lang.reflect.Method;

import cn.datax.common.utils.SpringContextHolder;
import org.springframework.util.ReflectionUtils;

import cn.hutool.core.util.StrUtil;

public class ScheduleRunnable implements Runnable {
	
	private Object target;
    private Method method;
    private String params;
    
    public ScheduleRunnable(String beanName, String methodName, String params) throws NoSuchMethodException, SecurityException {
    	this.target = SpringContextHolder.getBean(beanName);
        this.params = params;

        if (StrUtil.isNotEmpty(params)) {
        	this.method = target.getClass().getDeclaredMethod(methodName, String.class);
        } else {
        	this.method = target.getClass().getDeclaredMethod(methodName);
        }
    }

	@Override
	public void run() {
		try {
			ReflectionUtils.makeAccessible(method);
            if (StrUtil.isNotEmpty(params)) {
                method.invoke(target, params);
            } else {
                method.invoke(target);
            }
		} catch (Exception e) {
			 e.printStackTrace();
		}
	}
}
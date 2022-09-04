package com.alibaba.tesla.tkgone.server.common.annotation;

import com.alibaba.fastjson.JSONObject;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yangjinghua
 */
public class BasicAop {

    protected Map<String, Object> getParams(JoinPoint jp) {
        Map<String, Object> map = new HashMap<>(0);
        Object[] paramValues = jp.getArgs();
        String[] paramNames = ((CodeSignature)jp.getSignature()).getParameterNames();
        for (int index = 0; index < paramNames.length; index++) {
            map.put(paramNames[index], JSONObject.toJSON(paramValues[index]));
        }
        return map;
    }

    protected Method getObjMethod(JoinPoint joinPoint) throws ClassNotFoundException, NoSuchMethodException {
        String targetName = joinPoint.getTarget().getClass().getName();
        Class<?> targetClass = Class.forName(targetName);
        MethodSignature ms = (MethodSignature)joinPoint.getSignature();
        String methodName = ms.getMethod().getName();
        Class<?>[] par = ms.getParameterTypes();
        return targetClass.getMethod(methodName, par);
    }

}

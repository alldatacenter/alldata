package com.hw.lineage.server.interfaces.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * @description: ControllerAspect
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Aspect
@Component
public class ControllerAspect {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerAspect.class);

    @Around("execution(* com.hw.lineage.server.interfaces.controller..*.*(..)) ")
    public Object interceptor(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();

        if (signature.getMethod().getAnnotation(SkipAspect.class) != null) {
            return pjp.proceed();
        }

        String methodName = signature.getName();
        String method = signature.getDeclaringType().getSimpleName() + "." + methodName;

        // measure method execution time
        StopWatch stopWatch = new StopWatch(method);
        LOG.info("method: {}, param: {}", method, pjp.getArgs());
        stopWatch.start(methodName);
        Object result = pjp.proceed();
        stopWatch.stop();
        LOG.info("method: {}, time: {} ms, param: {}, result: {}", method, stopWatch.getTotalTimeMillis(), pjp.getArgs(), result);
        return result;
    }
}

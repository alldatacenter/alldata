package com.alibaba.sreworks.flyadmin.server.annotation.timediff;

import java.lang.reflect.Method;
import java.util.Map;

import com.alibaba.sreworks.flyadmin.server.annotation.BasicAop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * 计算方法调用时间切面
 *
 * @author yangjinghua
 */
@Aspect
@Component
@Slf4j
public class TimeDiffAop extends BasicAop {
    ThreadLocal<Long> time = new ThreadLocal<>();

    @Before("@annotation(com.alibaba.sreworks.flyadmin.server.annotation.timediff.TimeDiff)")
    public void beforeMethod(JoinPoint joinPoint) {
    }

    @Around("@annotation(com.alibaba.sreworks.flyadmin.server.annotation.timediff.TimeDiff)")
    public Object aroundMethod(ProceedingJoinPoint pjp) throws Throwable {
        time.set(System.currentTimeMillis());
        Method method = getObjMethod(pjp);
        String name = method.getAnnotation(TimeDiff.class).name();
        Map<String, Object> params = getParams(pjp);
        log.info("{} 开始执行", System.currentTimeMillis());
        return pjp.proceed();
    }

    @After("@annotation(com.alibaba.sreworks.flyadmin.server.annotation.timediff.TimeDiff)")
    public void afterMethod(JoinPoint joinPoint) {
        try {
            Method method = getObjMethod(joinPoint);
            String name = method.getAnnotation(TimeDiff.class).name();
            Map<String, Object> params = getParams(joinPoint);
            log.info(
                "{}执行结束，总耗时：{} ms",
                System.currentTimeMillis(),
                System.currentTimeMillis() - time.get()
            );
        } catch (Exception e) {
            log.error("打印总耗时出现异常：", e);
        }
    }

}


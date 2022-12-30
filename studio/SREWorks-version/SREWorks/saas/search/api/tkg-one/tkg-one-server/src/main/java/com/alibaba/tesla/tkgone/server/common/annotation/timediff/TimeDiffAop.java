package com.alibaba.tesla.tkgone.server.common.annotation.timediff;

import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.common.annotation.BasicAop;
import lombok.extern.log4j.Log4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 计算方法调用时间切面
 *
 * @author yangjinghua
 */
@Aspect
@Component
@Log4j
public class TimeDiffAop extends BasicAop {
    ThreadLocal<Long> time = new ThreadLocal<>();

    @Before("@annotation(com.alibaba.tesla.tkgone.server.common.annotation.timediff.TimeDiff)")
    public void beforeMethod(JoinPoint joinPoint) {
    }

    @Around("@annotation(com.alibaba.tesla.tkgone.server.common.annotation.timediff.TimeDiff)")
    public Object aroundMethod(ProceedingJoinPoint pjp) throws Throwable {
        time.set(System.currentTimeMillis());
        Method method = getObjMethod(pjp);
        String name = method.getAnnotation(TimeDiff.class).name();
        Map<String, Object> params = getParams(pjp);
        log.info(String.format("%s开始执行", Tools.processTemplateString(name, params)));
        return pjp.proceed();
    }

    @After("@annotation(com.alibaba.tesla.tkgone.server.common.annotation.timediff.TimeDiff)")
    public void afterMethod(JoinPoint joinPoint) {
        try {
            Method method = getObjMethod(joinPoint);
            String name = method.getAnnotation(TimeDiff.class).name();
            Map<String, Object> params = getParams(joinPoint);
            log.info(String.format("%s执行结束，总耗时：%sms", Tools.processTemplateString(name, params),
                System.currentTimeMillis() - time.get()));
        } catch (Exception e) {
            log.error("打印总耗时出现异常：", e);
        }
    }

}


package com.alibaba.sreworks.flyadmin.server.annotation.timediff;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 计算方法调用时间注解 name是业务名称描述，比如：调用下单接口
 * isCheckParams 是否校验接口入参，如果是会调用BeanValidator.validate(obj);进行参数的校验 Created by
 * yunpeng.zhao on 2017/8/15.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface TimeDiff {
    /**
     * 业务名称描述
     *
     * @return
     */
    String name() default "";

}

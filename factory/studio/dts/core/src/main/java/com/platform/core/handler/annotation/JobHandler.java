package com.platform.core.handler.annotation;

import java.lang.annotation.*;

/**
 * annotation for job handler
 * @author 2022-5-17 21:06:49
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JobHandler {

    String value() default "";

}

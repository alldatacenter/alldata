package cn.datax.common.security.annotation;

import java.lang.annotation.*;

/**
 * 服务调用不鉴权注解
 *
 * @author AllDataDC
 * @date 2023/01/30
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataInner {
    /**
     * 是否AOP统一处理
     *
     * @return false, true
     */
    boolean value() default true;
}

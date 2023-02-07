package cn.datax.common.dictionary.annotation;

import java.lang.annotation.*;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DictAop {

    /** 字典编码 */
    String code() default "";
}

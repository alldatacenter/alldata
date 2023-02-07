package cn.datax.common.mybatis.annotation;

import java.lang.annotation.*;

/**
 * 数据权限过滤注解
 * 参考 https://gitee.com/cancerGit/Crown
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DataScopeAop {

    /**
     * 表的别名
     */
    String alias() default "sys_user";
    /**
     * 表的部门字段
     */
    String deptScopeName() default "create_dept";
    /**
     * 表的用户字段
     */
    String userScopeName() default "create_by";
}

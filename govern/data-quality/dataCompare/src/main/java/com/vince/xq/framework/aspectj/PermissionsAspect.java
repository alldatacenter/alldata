package com.vince.xq.framework.aspectj;

import com.vince.xq.framework.context.PermissionContextHolder;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import com.vince.xq.common.utils.StringUtils;

/**
 * 自定义权限拦截器，将权限字符串放到当前请求中以便用于多个角色匹配符合要求的权限
 * 
 * @author ruoyi
 */
@Aspect
@Component
public class PermissionsAspect
{
    @Before("@annotation(controllerRequiresPermissions)")
    public void doBefore(JoinPoint point, RequiresPermissions controllerRequiresPermissions) throws Throwable
    {
        handleRequiresPermissions(point, controllerRequiresPermissions);
    }

    protected void handleRequiresPermissions(final JoinPoint joinPoint, RequiresPermissions requiresPermissions)
    {
        PermissionContextHolder.setContext(StringUtils.join(requiresPermissions.value(), ","));
    }
}

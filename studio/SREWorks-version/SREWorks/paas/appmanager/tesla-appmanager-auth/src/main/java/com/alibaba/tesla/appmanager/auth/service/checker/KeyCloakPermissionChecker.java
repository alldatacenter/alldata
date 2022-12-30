package com.alibaba.tesla.appmanager.auth.service.checker;

import com.alibaba.tesla.appmanager.domain.req.permission.CheckPermissionReq;
import com.alibaba.tesla.appmanager.domain.res.permission.CheckPermissionRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * KeyCloak Permission Checker
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "appmanager.auth.auth-engine", havingValue = "keycloak", matchIfMissing = true)
public class KeyCloakPermissionChecker implements PermissionChecker {

    /**
     * 检查权限列表
     *
     * @param request 权限检查请求
     * @return Response
     */
    @Override
    public CheckPermissionRes checkPermissions(CheckPermissionReq request) {
        return CheckPermissionRes.builder().permissions(request.getCheckPermissions()).build();
    }
}

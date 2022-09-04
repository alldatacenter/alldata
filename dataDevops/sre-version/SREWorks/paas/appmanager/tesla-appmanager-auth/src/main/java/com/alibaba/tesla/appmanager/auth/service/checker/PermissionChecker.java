package com.alibaba.tesla.appmanager.auth.service.checker;

import com.alibaba.tesla.appmanager.domain.req.permission.CheckPermissionReq;
import com.alibaba.tesla.appmanager.domain.res.permission.CheckPermissionRes;

/**
 * Permission Checker Interface
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface PermissionChecker {

    /**
     * 检查权限列表
     *
     * @param request 权限检查请求
     * @return Response
     */
    CheckPermissionRes checkPermissions(CheckPermissionReq request);
}

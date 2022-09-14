package com.alibaba.tesla.appmanager.auth.service;

import com.alibaba.tesla.appmanager.domain.req.permission.CheckPermissionReq;
import com.alibaba.tesla.appmanager.domain.res.permission.CheckPermissionRes;

/**
 * Permission 服务，用于校验权限
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface PermissionService {

    /**
     * 权限点检查
     *
     * @param request 检查请求
     * @return 持有的权限点列表
     */
    CheckPermissionRes checkPermission(CheckPermissionReq request);
}

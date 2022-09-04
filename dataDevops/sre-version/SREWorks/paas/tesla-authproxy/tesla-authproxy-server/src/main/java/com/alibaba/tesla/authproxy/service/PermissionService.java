package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyNoPermissionException;

public interface PermissionService {

    /**
     * 检查权限
     *
     * @param appId          APP ID
     * @param username       用户定位符 (对内: loginname, 对外: aliyunPk)
     * @param permissionPath 权限路径
     * @throws AuthProxyNoPermissionException 当无权限时抛出
     */
    void checkPermission(String appId, String username, String permissionPath)
        throws AuthProxyNoPermissionException;
}

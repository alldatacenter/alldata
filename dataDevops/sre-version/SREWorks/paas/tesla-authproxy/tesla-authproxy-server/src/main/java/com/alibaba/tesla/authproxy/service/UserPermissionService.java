package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.service.ao.UserPermissionCheckConditionAO;
import com.alibaba.tesla.authproxy.service.ao.UserPermissionCheckResultAO;
import com.alibaba.tesla.authproxy.service.ao.UserPermissionListConditionAO;
import com.alibaba.tesla.authproxy.service.ao.UserPermissionListResultAO;

public interface UserPermissionService {

    /**
     * 获取指定用户所拥有的权限列表
     *
     * @param condition 参数条件
     */
    UserPermissionListResultAO getPermissions(UserPermissionListConditionAO condition);

    /**
     * 检查指定用户拥有给定权限列表中的多少项权限
     *
     * @param condition 查询条件
     * @return 实际有权限的权限列表
     */
    UserPermissionCheckResultAO checkPermissions(UserPermissionCheckConditionAO condition);
}

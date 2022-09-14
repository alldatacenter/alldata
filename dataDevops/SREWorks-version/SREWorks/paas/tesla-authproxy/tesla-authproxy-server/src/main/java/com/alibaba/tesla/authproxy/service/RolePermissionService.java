package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.service.ao.*;

public interface RolePermissionService {

    /**
     * 根据指定的查询条件，查询当前的角色列表
     *
     * @param condition 查询条件
     */
    RolePermissionListResultAO list(RolePermissionListConditionAO condition);

    /**
     * 增加一个角色和权限的映射关系
     */
    void create(RolePermissionItemAO param);

    /**
     * 删除一个角色与权限的映射关系
     *
     * @return 删除个数
     */
    int delete(String tenantId, String roleId, String resourcePath);

    /**
     * 根据指定的查询条件，查询指定的权限 ID 对应的角色列表
     *
     * @param condition 查询条件
     */
    RoleListResultAO listRoleByPermission(PermissionRoleListConditionAO condition);
}

package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.service.ao.*;

/**
 * 角色服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface RoleService {

    /**
     * 获取指定角色对应了哪些用户
     *
     * @param condition 查询条件
     * @return
     */
    UserListResultAO listRoleUsers(RoleUserListConditionAO condition);

    /**
     * 获取指定角色对应了哪些部门
     *
     * @param condition 查询条件
     * @return
     */
    RoleDepartmentListResultAO listRoleDepartments(RoleDepartmentListConditionAO condition);

    /**
     * 根据指定的查询条件，查询当前的角色列表
     *
     * @param condition 查询条件
     */
    RoleListResultAO list(RoleListConditionAO condition);

    /**
     * 获取指定角色的详细信息
     *
     * @param condition 查询条件
     */
    RoleGetResultAO get(RoleGetConditionAO condition);

    /**
     * 创建一个角色
     */
    void create(RoleItemAO param);

    /**
     * 更新一个角色
     */
    void update(RoleItemAO param);

    /**
     * 删除一个角色
     */
    int delete(String tenantId, String roleId);

    /**
     * 授予指定的若干用户（增量）
     *
     * @param condition 增量用户
     * @return
     */
    void assignUsers(RoleUserAssigningConditionAO condition);
}

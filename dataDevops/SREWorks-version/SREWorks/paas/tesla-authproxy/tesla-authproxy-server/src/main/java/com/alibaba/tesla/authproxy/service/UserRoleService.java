package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.service.ao.UserRoleItemAO;
import com.alibaba.tesla.authproxy.service.ao.UserRoleListConditionAO;
import com.alibaba.tesla.authproxy.service.ao.UserRoleListResultAO;

public interface UserRoleService {

    /**
     * 根据指定的查询条件，查询当前指定用户的角色列表
     *
     * @param condition 查询条件
     */
    UserRoleListResultAO list(UserRoleListConditionAO condition);

    /**
     * 创建一个用户与角色的映射关系
     *
     * @param param 用户与角色映射关系
     */
    void create(UserRoleItemAO param);

    /**
     * 删除一个用户与角色的映射关系
     *
     * @param param 用户与角色映射关系
     */
    void delete(UserRoleItemAO param);
}

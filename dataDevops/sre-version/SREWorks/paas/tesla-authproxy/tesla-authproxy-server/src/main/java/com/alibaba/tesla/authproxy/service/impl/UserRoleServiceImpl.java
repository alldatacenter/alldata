package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.model.UserRoleRelDO;
import com.alibaba.tesla.authproxy.model.mapper.UserRoleRelMapper;
import com.alibaba.tesla.authproxy.service.UserRoleService;
import com.alibaba.tesla.authproxy.service.ao.UserRoleGetResultAO;
import com.alibaba.tesla.authproxy.service.ao.UserRoleItemAO;
import com.alibaba.tesla.authproxy.service.ao.UserRoleListConditionAO;
import com.alibaba.tesla.authproxy.service.ao.UserRoleListResultAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class UserRoleServiceImpl implements UserRoleService {

    private static final String LOG_PRE = "[" + UserRoleServiceImpl.class.getSimpleName() + "] ";

    @Autowired
    private UserRoleRelMapper userRoleRelMapper;

    /**
     * 根据指定的查询条件，查询当前指定用户的角色列表
     *
     * @param condition 查询条件
     */
    @Override
    public UserRoleListResultAO list(UserRoleListConditionAO condition) {
        String locale = condition.getLocale();
        String tenantId = condition.getTenantId();
        String userId = condition.getUserId();
        String appId = condition.getAppId();
        List<UserRoleRelDO> roles = userRoleRelMapper
            .findAllByTenantIdAndUserIdAndAppId(locale, tenantId, userId, appId);

        // 组装数据
        UserRoleListResultAO result = new UserRoleListResultAO();
        result.setTotal((long) roles.size());
        result.setItems(roles.stream()
            .map(UserRoleGetResultAO::from)
            .collect(Collectors.toList()));
        return result;
    }

    /**
     * 创建一个用户与角色的映射关系
     *
     * @param param 用户与角色映射关系
     */
    @Override
    public void create(UserRoleItemAO param) {
        userRoleRelMapper.insert(UserRoleRelDO.builder()
            .tenantId(param.getTenantId())
            .userId(param.getUserId())
            .roleId(param.getRoleId())
            .build());
    }

    /**
     * 删除一个用户与角色的映射关系
     *
     * @param param 用户与角色映射关系
     */
    @Override
    public void delete(UserRoleItemAO param) {
        String tenantId = param.getTenantId();
        String userId = param.getUserId();
        String roleId = param.getRoleId();
        userRoleRelMapper.deleteAllByTenantIdAndUserIdAndRoleId(tenantId, userId, roleId);
    }
}

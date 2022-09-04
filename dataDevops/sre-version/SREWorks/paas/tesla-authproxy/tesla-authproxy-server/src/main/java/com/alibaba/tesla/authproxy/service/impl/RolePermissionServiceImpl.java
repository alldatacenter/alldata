package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.model.RoleDO;
import com.alibaba.tesla.authproxy.model.RolePermissionRelDO;
import com.alibaba.tesla.authproxy.model.mapper.RolePermissionRelMapper;
import com.alibaba.tesla.authproxy.service.RolePermissionService;
import com.alibaba.tesla.authproxy.service.ao.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色权限服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class RolePermissionServiceImpl implements RolePermissionService {

    private static final String LOG_PRE = "[" + RolePermissionServiceImpl.class.getSimpleName() + "] ";

    @Autowired
    private RolePermissionRelMapper rolePermissionRelMapper;

    /**
     * 根据指定的查询条件，查询当前的角色列表
     *
     * @param condition 查询条件
     */
    @Override
    public RolePermissionListResultAO list(RolePermissionListConditionAO condition) {
        String tenantId = condition.getTenantId();
        String roleId = condition.getRoleId();
        List<RolePermissionRelDO> roles = rolePermissionRelMapper.findAllByRoleId(tenantId, roleId);

        // 组装数据
        RolePermissionListResultAO result = new RolePermissionListResultAO();
        result.setTotal((long) roles.size());
        result.setItems(roles.stream()
            .map(RolePermissionGetResultAO::from)
            .collect(Collectors.toList()));
        return result;
    }

    /**
     * 增加一个角色和权限的映射关系
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(RolePermissionItemAO param) {
        String tenantId = param.getTenantId();
        String roleId = param.getRoleId();
        String resourcePath = param.getResourcePath();
        String serviceCode = param.getServiceCode();

        // 简单判定
        if (rolePermissionRelMapper.get(tenantId, roleId, resourcePath) != null) {
            return;
        }

        RolePermissionRelDO item = RolePermissionRelDO.builder()
            .tenantId(tenantId)
            .roleId(roleId)
            .resourcePath(resourcePath)
            .serviceCode(serviceCode)
            .build();
        rolePermissionRelMapper.insert(item);
    }

    /**
     * 删除一个角色与权限的映射关系
     *
     * @return 删除个数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(String tenantId, String roleId, String resourcePath) {
        return rolePermissionRelMapper.delete(tenantId, roleId, resourcePath);
    }

    /**
     * 根据指定的查询条件，查询指定的权限 ID 对应的角色列表
     *
     * @param condition 查询条件
     */
    @Override
    public RoleListResultAO listRoleByPermission(PermissionRoleListConditionAO condition) {
        String tenantId = condition.getTenantId();
        String resourcePath = condition.getPermissionId();
        List<RoleDO> roles = rolePermissionRelMapper.findAllByResourcePath(tenantId, resourcePath);

        // 组装数据
        RoleListResultAO result = new RoleListResultAO();
        result.setTotal((long) roles.size());
        result.setItems(roles.stream()
            .map(RoleGetResultAO::from)
            .collect(Collectors.toList()));
        return result;
    }
}

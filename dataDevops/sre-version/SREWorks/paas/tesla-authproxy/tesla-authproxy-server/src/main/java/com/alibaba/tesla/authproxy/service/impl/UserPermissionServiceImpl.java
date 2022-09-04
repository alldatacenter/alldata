package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.lib.shiro.AuthProxyPrincipal;
import com.alibaba.tesla.authproxy.lib.shiro.DatabaseAuthorizingRealm;
import com.alibaba.tesla.authproxy.model.RolePermissionRelDO;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.model.mapper.RolePermissionRelMapper;
import com.alibaba.tesla.authproxy.model.mapper.UserMapper;
import com.alibaba.tesla.authproxy.service.UserPermissionService;
import com.alibaba.tesla.authproxy.service.ao.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户权限服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class UserPermissionServiceImpl implements UserPermissionService {

    private static final String LOG_PRE = "[" + UserPermissionServiceImpl.class.getSimpleName() + "] ";

    @Autowired
    private RolePermissionRelMapper rolePermissionRelMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取指定用户所拥有的权限列表
     *
     * @param condition 参数条件
     */
    @Override
    public UserPermissionListResultAO getPermissions(UserPermissionListConditionAO condition) {
        String tenantId = condition.getTenantId();
        String appId = condition.getAppId();
        String userId = condition.getUserId();
        List<RolePermissionRelDO> permissions = rolePermissionRelMapper
            .findAllByUserIdAndRoleIdPrefix(tenantId, userId, appId);

        // 组装数据
        UserPermissionListResultAO result = new UserPermissionListResultAO();
        result.setTotal((long) permissions.size());
        result.setItems(permissions.stream()
            .map(UserPermissionGetResultAO::from)
            .collect(Collectors.toList()));
        return result;
    }

    /**
     * 检查指定用户拥有给定权限列表中的多少项权限
     *
     * @param condition 查询条件
     * @return 实际有权限的权限列表
     */
    @Override
    public UserPermissionCheckResultAO checkPermissions(UserPermissionCheckConditionAO condition) {
        // PaaS 侧全部放开
        if ("PaaS".equals(System.getenv("CLOUD_TYPE"))) {
            return UserPermissionCheckResultAO.builder().permissions(condition.getCheckPermissions()).build();
        }

        String userId = condition.getUserId();
        List<String> checkPermissions = condition.getCheckPermissions();

        // 获取当前用户信息
        UserDO user = userMapper.getByUserId(userId);
        if (user == null) {
            log.info("Cannot get user by userId {}, skip permission checker", userId);
            return UserPermissionCheckResultAO.builder().permissions(new ArrayList<>()).build();
        }

        // 获取当前用户权限信息
        AuthProxyPrincipal principal = AuthProxyPrincipal.builder()
            .tenantId(condition.getTenantId())
            .userId(userId)
            .appId(condition.getAppId())
            .depId(user.getDepId())
            .asRole(condition.getAsRole())
            .defaultPermissions(condition.getDefaultPermissions())
            .build();
        Subject subject = new Subject.Builder()
            .principals(new SimplePrincipalCollection(principal, DatabaseAuthorizingRealm.REALM_NAME))
            .buildSubject();

        // 获取实际有权限的权限列表
        List<String> finalPermissions = new ArrayList<>();
        boolean[] permittedList = subject.isPermitted(checkPermissions.toArray(new String[0]));
        for (int i = 0; i < checkPermissions.size(); i++) {
            if (permittedList[i]) {
                finalPermissions.add(checkPermissions.get(i));
            }
        }
        return UserPermissionCheckResultAO.builder().permissions(finalPermissions).build();
    }
}

package com.alibaba.tesla.authproxy.lib.shiro;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.model.RolePermissionRelDO;
import com.alibaba.tesla.authproxy.model.UserRoleRelDO;
import com.alibaba.tesla.authproxy.model.mapper.UserRoleRelMapper;
import com.alibaba.tesla.authproxy.model.repository.RolePermissionRelRepository;
import com.alibaba.tesla.authproxy.util.PermissionUtil;
import com.alibaba.tesla.common.base.util.TeslaGsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DatabaseAuthorizingRealm extends AuthorizingRealm {

    private static final String LOG_PRE = "[" + DatabaseAuthorizingRealm.class.getSimpleName()
        + "] action=realm||message=";

    public static final String REALM_NAME = "abmRealm";

    @Autowired
    private RolePermissionRelRepository rolePermissionRelRepository;

    @Autowired
    private UserRoleRelMapper userRoleRelMapper;

    /**
     * 设置 realm 名称
     */
    public DatabaseAuthorizingRealm() {
        super();
        this.setName(REALM_NAME);
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return true;
    }

    /**
     * 获取用户的授权信息 (roles && permissions)
     *
     * @param principalCollection 定位信息
     * @return 授权信息
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        // Principal 构造：$tenantId::$userId
        AuthProxyPrincipal principal = (AuthProxyPrincipal) principalCollection.getPrimaryPrincipal();
        String tenantId = principal.getTenantId();
        String userId = principal.getUserId();
        String appId = principal.getAppId();
        String depId = principal.getDepId();
        String asRole = principal.getAsRole();
        List<String> defaultPermissions = principal.getDefaultPermissions();

        // 获取当前用户对应的角色列表与权限列表
        Set<String> roles = userRoleRelMapper
            .findAllByTenantIdAndUserIdAndAppId(Constants.DEFAULT_LOCALE, tenantId, userId, appId)
            .stream()
            .map(UserRoleRelDO::getRoleId)
            .collect(Collectors.toSet());
        log.info(LOG_PRE + "get roles (stage1)||tenantId={}||userId={}||appId={}||roles={}",
            tenantId, userId, appId, TeslaGsonUtil.toJson(roles));
        roles.addAll(userRoleRelMapper
            .findAllByTenantIdAndUserIdAndAppId(Constants.DEFAULT_LOCALE, tenantId,
                PermissionUtil.getDepUserId(depId), appId)
            .stream()
            .map(UserRoleRelDO::getRoleId)
            .collect(Collectors.toSet()));
        roles.add(PermissionUtil.getGuestRole(appId));
        log.info(LOG_PRE + "get roles (stage2)||tenantId={}||userId={}||appId={}||roles={}",
            tenantId, userId, appId, TeslaGsonUtil.toJson(roles));

        // 当以其他角色身份进行认证的时候，判定该角色在当前状态下是否可用，如果可用，仅保留该角色，如果不可用，则全部删除
        if (!StringUtils.isEmpty(asRole)) {
            if (roles.contains(asRole)) {
                roles = new HashSet<>();
                roles.add(asRole);
            } else {
                roles = new HashSet<>();
            }
        }
        log.info(LOG_PRE + "get roles (stage3)||tenantId={}||userId={}||appId={}||roles={}||asRole={}",
            tenantId, userId, appId, TeslaGsonUtil.toJson(roles), asRole);

        // 获取对应的角色包含的权限清单
        Set<String> permissions = new HashSet<>();
        if (roles.size() > 0) {
            List<RolePermissionRelDO> rolePermissions = rolePermissionRelRepository
                .findAllByTenantIdAndRoleIdIn(tenantId, roles);
            permissions = rolePermissions.stream()
                .map(RolePermissionRelDO::getResourcePath)
                .collect(Collectors.toSet());
        }
        permissions.addAll(defaultPermissions);
        log.info(LOG_PRE + "get roles and permissions from user||tenantId={}||userId={}||appId={}||roles={}" +
                "||permissions={}", tenantId, userId, appId, TeslaGsonUtil.toJson(roles),
            TeslaGsonUtil.toJson(permissions));

        // 构造 Authorization Info 对象
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        authorizationInfo.setRoles(roles);
        authorizationInfo.setStringPermissions(permissions);
        return authorizationInfo;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken)
        throws AuthenticationException {
        AuthProxyPrincipal principal = (AuthProxyPrincipal) authenticationToken.getPrincipal();
        return new SimpleAuthenticationInfo(principal, principal, REALM_NAME);
    }
}

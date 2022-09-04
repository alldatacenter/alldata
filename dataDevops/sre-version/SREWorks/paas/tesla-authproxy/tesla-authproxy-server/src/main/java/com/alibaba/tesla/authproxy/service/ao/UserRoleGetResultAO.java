package com.alibaba.tesla.authproxy.service.ao;

import com.alibaba.tesla.authproxy.model.UserRoleRelDO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRoleGetResultAO {

    private String tenantId;
    private String roleId;
    private String roleName;
    private String roleDescription;

    /**
     * 根据 UserRoleRelDO 构造当前返回内容数据
     *
     * @param role 角色对象
     * @return UserRoleGetResultAO
     */
    public static UserRoleGetResultAO from(UserRoleRelDO role) {
        return UserRoleGetResultAO.builder()
            .tenantId(role.getTenantId())
            .roleId(role.getRoleId())
            .roleName(role.getRoleName())
            .roleDescription(role.getRoleDescription())
            .build();
    }
}

package com.alibaba.tesla.authproxy.service.ao;

import com.alibaba.tesla.authproxy.model.RolePermissionRelDO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RolePermissionGetResultAO {

    private String tenantId;
    private String serviceCode;
    private String roleId;
    private String resourcePath;

    /**
     * 根据 RolePermissionRelDO 构造当前返回内容数据
     *
     * @param rel 角色权限对象
     * @return UserRoleGetResultAO
     */
    public static RolePermissionGetResultAO from(RolePermissionRelDO rel) {
        return RolePermissionGetResultAO.builder()
            .tenantId(rel.getTenantId())
            .serviceCode(rel.getServiceCode())
            .roleId(rel.getRoleId())
            .resourcePath(rel.getResourcePath())
            .build();
    }
}

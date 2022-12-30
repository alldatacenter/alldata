package com.alibaba.tesla.authproxy.service.ao;

import com.alibaba.tesla.authproxy.model.RolePermissionRelDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionGetResultAO {

    private String tenantId;
    private String serviceCode;
    private String roleId;
    private String resourcePath;

    /**
     * 根据 RolePermissionRelDO 构造当前返回内容数据
     *
     * @param permission 权限对象
     * @return UserPermissionGetResultAO
     */
    public static UserPermissionGetResultAO from(RolePermissionRelDO permission) {
        return UserPermissionGetResultAO.builder()
            .tenantId(permission.getTenantId())
            .serviceCode(permission.getServiceCode())
            .roleId(permission.getRoleId())
            .resourcePath(permission.getResourcePath())
            .build();
    }
}

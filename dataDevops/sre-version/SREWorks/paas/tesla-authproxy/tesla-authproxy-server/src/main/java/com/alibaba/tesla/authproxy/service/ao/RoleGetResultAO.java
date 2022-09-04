package com.alibaba.tesla.authproxy.service.ao;

import com.alibaba.tesla.authproxy.model.RoleDO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleGetResultAO {

    private String tenantId;
    private String roleId;
    private String name;
    private String description;
    private Long gmtCreate;
    private Long gmtModified;

    /**
     * 根据 RoleDO 构造当前的返回结构内容
     *
     * @param role 角色详情
     * @return RoleGetResultAO
     */
    public static RoleGetResultAO from(RoleDO role) {
        return RoleGetResultAO.builder()
            .tenantId(role.getTenantId())
            .roleId(role.getRoleId())
            .name(role.getName())
            .description(role.getDescription())
            .gmtCreate(role.getGmtCreate().toEpochSecond())
            .gmtModified(role.getGmtModified().toEpochSecond())
            .build();
    }
}

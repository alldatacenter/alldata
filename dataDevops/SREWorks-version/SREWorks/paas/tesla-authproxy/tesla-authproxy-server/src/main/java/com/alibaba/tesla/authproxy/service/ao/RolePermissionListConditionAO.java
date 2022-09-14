package com.alibaba.tesla.authproxy.service.ao;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RolePermissionListConditionAO {

    private String tenantId;
    private String roleId;
}

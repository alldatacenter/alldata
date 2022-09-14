package com.alibaba.tesla.authproxy.service.ao;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RolePermissionItemAO {

    private String tenantId;
    private String roleId;
    private String resourcePath;
    private String serviceCode;
}

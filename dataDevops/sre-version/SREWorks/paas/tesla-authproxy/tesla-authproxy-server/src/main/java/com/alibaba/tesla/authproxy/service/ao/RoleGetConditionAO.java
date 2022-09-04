package com.alibaba.tesla.authproxy.service.ao;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleGetConditionAO {

    private String locale;
    private String tenantId;
    private String appId;
    private String roleId;
}

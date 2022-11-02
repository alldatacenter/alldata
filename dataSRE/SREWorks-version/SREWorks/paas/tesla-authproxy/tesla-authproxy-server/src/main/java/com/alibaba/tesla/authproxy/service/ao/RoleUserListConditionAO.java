package com.alibaba.tesla.authproxy.service.ao;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleUserListConditionAO {

    private String locale;
    private String appId;
    private String tenantId;
    private String roleId;
}

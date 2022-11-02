package com.alibaba.tesla.authproxy.service.ao;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleDepartmentListConditionAO {

    private String tenantId;
    private String locale;
    private String roleId;
}

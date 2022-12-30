package com.alibaba.tesla.authproxy.service.ao;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRoleItemAO {

    private String tenantId;
    private String userId;
    private String roleId;
}

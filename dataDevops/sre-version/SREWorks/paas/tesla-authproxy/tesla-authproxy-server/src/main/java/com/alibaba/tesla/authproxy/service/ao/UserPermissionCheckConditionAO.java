package com.alibaba.tesla.authproxy.service.ao;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserPermissionCheckConditionAO {

    private String tenantId;
    private String appId;
    private String userId;
    private String asRole;
    private List<String> defaultPermissions;
    private List<String> checkPermissions;
}

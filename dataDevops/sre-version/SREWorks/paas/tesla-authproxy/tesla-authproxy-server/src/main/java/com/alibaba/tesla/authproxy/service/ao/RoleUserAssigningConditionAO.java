package com.alibaba.tesla.authproxy.service.ao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleUserAssigningConditionAO {

    private String locale;
    private String appId;
    private String tenantId;
    private String roleId;
    private List<String> userIds;
}

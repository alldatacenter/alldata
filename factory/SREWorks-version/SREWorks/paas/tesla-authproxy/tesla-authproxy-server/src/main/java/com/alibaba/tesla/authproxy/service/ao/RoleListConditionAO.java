package com.alibaba.tesla.authproxy.service.ao;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleListConditionAO {

    private String tenantId;
    private String appId;
    private String locale;
    private String roleId;
    private String name;

    /**
     * 公共参数
     */
    private String sortBy;
    private String orderBy;
    private Integer page;
    private Integer pageSize;
}

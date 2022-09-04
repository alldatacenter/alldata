package com.alibaba.tesla.gateway.server.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SwitchViewUser {

    /**
     * 工号
     */
    private String empId;

    /**
     * 用户名称
     */
    private String loginName;

    /**
     * Buc ID
     */
    private String bucId;
}

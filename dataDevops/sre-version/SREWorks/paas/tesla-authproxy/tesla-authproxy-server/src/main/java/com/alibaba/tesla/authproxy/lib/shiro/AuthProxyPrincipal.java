package com.alibaba.tesla.authproxy.lib.shiro;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 权代默认 Principal 对象
 */
@Data
@Builder
public class AuthProxyPrincipal {

    private String tenantId;
    private String userId;
    private String appId;
    private String depId;
    private String asRole;
    private List<String> defaultPermissions;
}

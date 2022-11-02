package com.alibaba.tesla.appmanager.domain.req.deployconfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployConfigApplyTemplateReq {

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 环境 ID
     */
    private String envId;

    /**
     * API 版本
     */
    private String apiVersion;

    /**
     * 配置内容
     */
    private String config;

    /**
     * 是否开启
     */
    private boolean enabled = false;

    /**
     * Namespace ID
     */
    private String isolateNamespaceId;

    /**
     * Stage ID
     */
    private String isolateStageId;

    /**
     * Product ID
     */
    private String productId;

    /**
     * Release ID
     */
    private String releaseId;
}

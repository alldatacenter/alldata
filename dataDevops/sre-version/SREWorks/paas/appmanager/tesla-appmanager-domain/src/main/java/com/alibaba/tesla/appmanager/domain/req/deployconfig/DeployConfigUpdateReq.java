package com.alibaba.tesla.appmanager.domain.req.deployconfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployConfigUpdateReq {

    /**
     * API 版本
     */
    private String apiVersion;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 类型 ID
     */
    private String typeId;

    /**
     * 环境 ID
     */
    private String envId;

    /**
     * 配置信息
     */
    private String config;

    /**
     * 是否继承
     */
    private boolean inherit;

    /**
     * Namespace ID
     */
    private String isolateNamespaceId;

    /**
     * Stage ID
     */
    private String isolateStageId;

    /**
     * 归属产品 ID
     */
    private String productId;

    /**
     * 归属发布版本 ID
     */
    private String releaseId;
}

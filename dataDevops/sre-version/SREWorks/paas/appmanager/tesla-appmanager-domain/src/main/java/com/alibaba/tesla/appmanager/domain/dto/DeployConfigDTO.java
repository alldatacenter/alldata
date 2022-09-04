package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 部署配置表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeployConfigDTO {
    /**
     * ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

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
     * API Version
     */
    private String apiVersion;

    /**
     * 当前版本
     */
    private Integer currentRevision;

    /**
     * 是否开启
     */
    private Boolean enabled;

    /**
     * 配置内容，允许包含 Jinja
     */
    private String config;

    /**
     * 是否继承
     */
    private Boolean inherit;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * 产品 ID
     */
    private String productId;

    /**
     * 发布版本 ID
     */
    private String releaseId;
}
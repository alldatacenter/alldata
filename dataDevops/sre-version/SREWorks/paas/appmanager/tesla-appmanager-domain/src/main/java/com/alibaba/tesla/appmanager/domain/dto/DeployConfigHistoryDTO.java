package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 部署配置历史表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeployConfigHistoryDTO {
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
     * 历史版本号
     */
    private Integer revision;

    /**
     * 配置内容，允许包含 Jinja
     */
    private String config;

    /**
     * 是否继承
     */
    private Boolean inherit;

    /**
     * 修改者
     */
    private String modifier;

    /**
     * 是否删除
     */
    private Boolean deleted;
}
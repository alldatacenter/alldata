package com.alibaba.tesla.appmanager.plugin.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Plugin 资源表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PluginResourceDO {
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
     * Plugin 唯一标识
     */
    private Long pluginName;

    /**
     * Plugin 版本 (SemVer)
     */
    private String pluginVersion;

    /**
     * Plugin 资源部署到的目标 Cluster ID
     */
    private String clusterId;

    /**
     * Plugin 资源状态 (PENDING/RUNNING/SUCCESS/FAILURE/REMOVED)
     */
    private String instanceStatus;

    /**
     * Plugin 资源部署错误信息
     */
    private String instanceErrorMessage;

    /**
     * 是否已安装注册
     */
    private Boolean instanceRegistered;

    /**
     * 乐观锁版本
     */
    private Integer lockVersion;
}
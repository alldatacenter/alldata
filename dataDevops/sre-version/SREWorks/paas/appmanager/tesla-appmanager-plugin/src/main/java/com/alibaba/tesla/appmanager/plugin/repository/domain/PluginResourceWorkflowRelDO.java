package com.alibaba.tesla.appmanager.plugin.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Plugin 资源关联 Workflow 表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PluginResourceWorkflowRelDO {
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
     * Plugin Resource ID
     */
    private Long pluginResourceId;

    /**
     * Plugin 资源部署到的目标 Cluster ID
     */
    private String clusterId;

    /**
     * Plugin 资源对应的 Workflow 类型 (INSTALL / UNINSTALL)
     */
    private String workflowType;

    /**
     * Plugin 资源对应的 Workflow ID
     */
    private Long workflowId;

    /**
     * Plugin 资源对应的 Workflow 状态
     */
    private String workflowStatus;

    /**
     * Plugin 资源对应的 Workflow 错误信息
     */
    private String workflowErrorMessage;

    /**
     * 乐观锁版本
     */
    private Integer lockVersion;
}
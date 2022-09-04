package com.alibaba.tesla.appmanager.workflow.repository.domain;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Workflow 类型定义表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowDefinitionDO {
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
     * Workflow 类型唯一标识
     */
    private String workflowType;

    /**
     * 动态脚本 Kind
     */
    private String dsKind;

    /**
     * 动态脚本 Name
     */
    private String dsName;

    /**
     * 动态脚本版本
     */
    private Integer dsRevision;
}
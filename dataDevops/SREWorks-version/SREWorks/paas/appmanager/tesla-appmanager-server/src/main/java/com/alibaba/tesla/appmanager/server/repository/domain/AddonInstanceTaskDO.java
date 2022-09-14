package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddonInstanceTaskDO implements Serializable {
    /**
     * 主键
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 修改时间
     */
    private Date gmtModified;

    /**
     * 命名空间 ID
     */
    private String namespaceId;

    /**
     * 组件ID
     */
    private String addonId;

    /**
     * 组件实例名称
     */
    private String addonName;

    /**
     * 组件版本
     */
    private String addonVersion;

    /**
     * 附加组件属性
     */
    private String addonAttrs;

    /**
     * 状态
     */
    private String taskStatus;

    /**
     * 错误信息
     */
    private String taskErrorMessage;

    /**
     * 处理流程ID
     */
    private Long taskProcessId;

    /**
     * 任务申请 spec 定义信息 (对应 addon 的 workload spec 部分)
     */
    private String taskExt;

    private static final long serialVersionUID = 1L;
}
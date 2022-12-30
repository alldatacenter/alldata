package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 组件包创建任务详情表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComponentPackageTaskDO {
    private static final long serialVersionUID = 1L;
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
     * 应用唯一标识
     */
    private String appId;

    /**
     * 组件类型
     */
    private String componentType;

    /**
     * 组件类型下的唯一组件标识
     */
    private String componentName;

    /**
     * 版本号
     */
    private String packageVersion;

    /**
     * 存储位置相对路径
     */
    private String packagePath;

    /**
     * 创建者
     */
    private String packageCreator;

    /**
     * 包 MD5
     */
    private String packageMd5;

    /**
     * 任务状态
     */
    private String taskStatus;

    /**
     * 映射 component package 表主键 ID
     */
    private Long componentPackageId;

    /**
     * 应用包任务ID
     */
    private Long appPackageTaskId;

    /**
     * VERSION
     */
    private Integer version;

    /**
     * 环境 ID
     */
    private String envId;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * 包 Addon 描述信息
     */
    private String packageAddon;

    /**
     * 包配置选项信息
     */
    private String packageOptions;

    /**
     * 扩展信息 JSON
     */
    private String packageExt;

    /**
     * 任务日志
     */
    private String taskLog;
}
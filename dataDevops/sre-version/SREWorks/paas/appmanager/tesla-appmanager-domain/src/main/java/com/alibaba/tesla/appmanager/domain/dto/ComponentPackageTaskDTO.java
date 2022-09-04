package com.alibaba.tesla.appmanager.domain.dto;

import java.util.Date;

import com.alibaba.fastjson.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Component Package 任务详细信息
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPackageTaskDTO {

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
     * 包 Addon 描述信息
     */
    private JSONObject packageAddon;

    /**
     * 包配置选项信息
     */
    @JsonIgnore
    private JSONObject packageOptions;

    /**
     * 扩展信息 JSON
     */
    private String packageExt;

    /**
     * 任务状态
     */
    private String taskStatus;

    /**
     * 任务日志
     */
    private String taskLog;

    /**
     * 映射 component package 表主键 ID
     */
    private Long componentPackageId;

    /**
     * 应用包任务ID
     */
    private Long appPackageTaskId;

    /**
     * 简易版本号
     */
    private String simplePackageVersion;

    /**
     * 环境 ID
     */
    private String envId;
}

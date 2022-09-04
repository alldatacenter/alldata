package com.alibaba.tesla.appmanager.plugin.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Plugin 定义表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PluginDefinitionDO {
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
    private String pluginName;

    /**
     * Plugin 版本 (SemVer)
     */
    private String pluginVersion;

    /**
     * 是否已安装注册
     */
    private Boolean pluginRegistered;

    /**
     * Plugin 包路径
     */
    private String packagePath;

    /**
     * Plugin 描述
     */
    private String pluginDescription;

    /**
     * Plugin 依赖 (JSON Array)
     */
    private String pluginDependencies;

    /**
     * Plugin 附加信息 (JSON Object)
     */
    private String pluginExtra;
}
package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 组件包详情表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComponentPackageDO implements Serializable {
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
     * 组件类型
     */
    private String componentType;

    /**
     * 组件类型下的唯一组件标识
     */
    private String componentName;

    /**
     * 包 MD5
     */
    private String packageMd5;

    /**
     * VERSION
     */
    private Integer version;

    /**
     * 包的 ComponentSchema 信息 (YAML)
     */
    private String componentSchema;

    /**
     * 当前 Component Package 的 Addon 依赖关系配置 (JSON)
     */
    private String packageAddon;

    /**
     * 当前 Component Package 在构建时传入的 options 配置信息 (JSON)
     */
    private String packageOptions;

    private static final long serialVersionUID = 1L;
}
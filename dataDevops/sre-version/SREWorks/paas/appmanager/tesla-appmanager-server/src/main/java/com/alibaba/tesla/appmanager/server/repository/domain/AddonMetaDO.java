package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 附加组件元信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddonMetaDO {
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
     * 类型（可选 CORE-SERVICE / THIRDPARTY）
     */
    private String addonType;

    /**
     * 附加组件唯一标识
     */
    private String addonId;

    /**
     * 版本号
     */
    private String addonVersion;

    /**
     * 附加组件名
     */
    private String addonLabel;

    /**
     * 附加组件描述
     */
    private String addonDescription;

    /**
     * 附加组件定义 Schema
     */
    private String addonSchema;

    /**
     * 附件组件配置Schema
     */
    private String addonConfigSchema;
}
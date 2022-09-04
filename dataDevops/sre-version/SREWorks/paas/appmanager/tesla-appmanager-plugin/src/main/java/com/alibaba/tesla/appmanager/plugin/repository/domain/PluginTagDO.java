package com.alibaba.tesla.appmanager.plugin.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Plugin 标签表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PluginTagDO {
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
     * 所属 Plugin ID
     */
    private Long pluginId;

    /**
     * 标签
     */
    private String tag;
}
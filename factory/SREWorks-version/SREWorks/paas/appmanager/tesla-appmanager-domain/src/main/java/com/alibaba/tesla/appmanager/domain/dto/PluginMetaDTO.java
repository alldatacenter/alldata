package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author jiongen.zje@alibaba-inc.com
 * @date 2022/08/03.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginMetaDTO {
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
     * 插件名称
     */
    private String pluginName;
    /**
     * 插件版本
     */
    private String pluginVersion;

    /**
     * 插件版本
     */
    private String pluginDescription;
    /**
     * Plugin 包路径
     */
    private String packagePath;

}

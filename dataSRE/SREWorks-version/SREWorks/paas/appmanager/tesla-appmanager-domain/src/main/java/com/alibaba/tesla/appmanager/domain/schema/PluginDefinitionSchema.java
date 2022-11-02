package com.alibaba.tesla.appmanager.domain.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * 应用包 Schema
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDefinitionSchema implements Schema, Serializable {

    /**
     * 名称
     */
    private String pluginName;

    /**
     * 版本
     */
    private String pluginVersion;

    /**
     * 描述
     */
    private String pluginDescription;

}

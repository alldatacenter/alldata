package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 组件定义表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComponentDO {

    private Long id;

    /**
     * 组件类型，全局唯一
     */
    private String componentType;

    /**
     * 适配类型，可选 core, groovy
     */
    private String componentAdapterType;

    /**
     * 当前版本
     */
    private Integer currentRevision;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * 适配器值，core 时可为空，groovy 时为脚本引用名称
     */
    private String componentAdapterValue;
}
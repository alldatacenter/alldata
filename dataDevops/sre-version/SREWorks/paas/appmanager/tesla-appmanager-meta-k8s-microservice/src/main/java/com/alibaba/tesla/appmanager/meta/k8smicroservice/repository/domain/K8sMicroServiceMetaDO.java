package com.alibaba.tesla.appmanager.meta.k8smicroservice.repository.domain;

import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * k8s微应用元信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class K8sMicroServiceMetaDO {
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
     * 应用唯一标识
     */
    private String appId;

    /**
     * 微服务标示
     */
    private String microServiceId;

    /**
     * 微服务名称
     */
    private String name;

    /**
     * 微服务描述
     */
    private String description;

    /**
     * 组件类型
     */
    private ComponentTypeEnum componentType;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * 架构
     */
    private String arch;

    /**
     * 扩展信息
     */
    private String microServiceExt;

    /**
     * options
     */
    private String options;
}
package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 应用关联组件 DTO
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppComponentDTO {

    private Long id;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * 组件描述
     */
    private String componentLabel;

    /**
     * 组件类型
     */
    private ComponentTypeEnum componentType;

    /**
     * 组件唯一标示
     */
    private String componentName;

    /**
     * 组件版本
     */
    private String componentVersion;
}

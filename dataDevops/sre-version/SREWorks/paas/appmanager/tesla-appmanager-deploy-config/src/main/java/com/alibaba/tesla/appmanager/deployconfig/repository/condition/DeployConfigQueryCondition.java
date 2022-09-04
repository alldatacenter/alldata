package com.alibaba.tesla.appmanager.deployconfig.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DeployConfigQueryCondition extends BaseCondition {

    /**
     * ID
     */
    private Long id;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 类型 ID
     */
    private String typeId;

    /**
     * 环境 ID
     */
    private String envId;

    /**
     * API 版本
     */
    private String apiVersion;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 是否继承
     */
    private Boolean inherit;

    /**
     * Namespace ID
     */
    private String isolateNamespaceId;

    /**
     * Namespace ID 不等条件
     */
    private String isolateNamespaceIdNotEqualTo;

    /**
     * Stage ID
     */
    private String isolateStageId;

    /**
     * Stage ID 不等条件
     */
    private String isolateStageIdNotEqualTo;
}

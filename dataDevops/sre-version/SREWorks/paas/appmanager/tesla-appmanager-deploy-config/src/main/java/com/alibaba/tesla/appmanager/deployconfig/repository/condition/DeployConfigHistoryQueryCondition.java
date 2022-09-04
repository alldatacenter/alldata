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
public class DeployConfigHistoryQueryCondition extends BaseCondition {

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
     * 历史修订版本号
     */
    private Integer revision;

    /**
     * 是否继承
     */
    private Boolean inherit;

    /**
     * Namespace ID
     */
    private String isolateNamespaceId;

    /**
     * Stage ID
     */
    private String isolateStageId;
}

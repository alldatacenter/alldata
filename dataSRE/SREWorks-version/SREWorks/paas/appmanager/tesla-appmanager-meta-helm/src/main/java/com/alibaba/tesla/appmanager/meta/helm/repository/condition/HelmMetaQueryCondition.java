package com.alibaba.tesla.appmanager.meta.helm.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Helm Meta 查询条件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HelmMetaQueryCondition extends BaseCondition {
    /**
     * ID
     */
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
     * Namespace ID 不等条件
     */
    private String namespaceIdNotEqualTo;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * Stage ID 不等条件
     */
    private String stageIdNotEqualTo;

    /**
     * Helm 名称
     */
    private String name;

    /**
     * Helm packageId
     */
    private String helmPackageId;
}

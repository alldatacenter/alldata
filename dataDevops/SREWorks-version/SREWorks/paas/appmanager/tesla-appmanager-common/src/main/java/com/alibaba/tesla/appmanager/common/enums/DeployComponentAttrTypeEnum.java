package com.alibaba.tesla.appmanager.common.enums;

/**
 * 部署工单 Component 扩展属性类型 Enum
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum DeployComponentAttrTypeEnum {

    /**
     * 部署过程中的 ComponentSchema 数据 (YAML)
     */
    COMPONENT_SCHEMA,

    /**
     * 部署过程中的 TraitSchema 数据 (YAML)
     */
    TRAIT_SCHEMA,

    /**
     * Trait 绑定的 Component 的 Workload 内容 (YAML)
     */
    TRAIT_COMPONENT_WORKLOAD,

    /**
     * 部署过程中的 ComponentOptions 数据 (YAML)
     */
    OPTIONS,

    /**
     * 部署过程中的 DataOutputs 数据 (JSON)
     */
    DATA_OUTPUTS,

    /**
     * 部署过程中的全局参数 (JSON)
     */
    GLOBAL_PARAMS,

    /**
     * 部署过程中的全局常量 (JSON)
     */
    GLOBAL_VARIABLES,

    /**
     * Owner Reference
     */
    OWNER_REFERENCES;
}

package com.alibaba.tesla.appmanager.common.enums;

/**
 * 动态脚本类型枚举
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum DynamicScriptKindEnum {

    /**
     * 部署微服务组件
     */
    DEPLOY_MICROSERVICE_COMPONENT,

    /**
     * 构建微服务组件
     */
    BUILD_MICROSERVICE_COMPONENT,

    /**
     * 部署 Job 组件
     */
    DEPLOY_JOB_COMPONENT,

    /**
     * 构建 Job 组件
     */
    BUILD_JOB_COMPONENT,

    /**
     * 构建 Helm 组件
     */
    BUILD_HELM_COMPONENT,

    /**
     * 部署 Helm 组件
     */
    DEPLOY_HELM_COMPONENT,

    /**
     * 构建 ABM Chart 模块
     */
    BUILD_ABM_CHART_COMPONENT,

    /**
     * 部署 ABM Chart 模块
     */
    DEPLOY_ABM_CHART_COMPONENT,

    /**
     * 构建 ASI 模块
     */
    BUILD_ASI_COMPONENT,

    /**
     * 部署 ASI 模块
     */
    DEPLOY_ASI_COMPONENT,

    /**
     * 构建 ABM Kustomize 模块
     */
    BUILD_ABM_KUSTOMIZE_COMPONENT,

    /**
     * 部署 ABM Kustomize 模块
     */
    DEPLOY_ABM_KUSTOMIZE_COMPONENT,

    /**
     * 构建 ABM Helm 模块
     */
    BUILD_ABM_HELM_COMPONENT,

    /**
     * 部署 ABM Helm 模块
     */
    DEPLOY_ABM_HELM_COMPONENT,

    /**
     * 构建 ABM Status 模块
     */
    BUILD_ABM_STATUS_COMPONENT,

    /**
     * 部署 ABM Status 模块
     */
    DEPLOY_ABM_STATUS_COMPONENT,

    /**
     * 构建 ABM ES Status 模块
     */
    BUILD_ABM_ES_STATUS_COMPONENT,

    /**
     * 部署 ABM ES Status 模块
     */
    DEPLOY_ABM_ES_STATUS_COMPONENT,

    /**
     * 通用构建 Resource Addon 模块
     */
    BUILD_RESOURCE_ADDON_COMPONENT,

    /**
     * 通用部署 Resource Addon 模块
     */
    DEPLOY_RESOURCE_ADDON_COMPONENT,

    /**
     * 构建 ProductOps V2 组件
     */
    BUILD_IA_V2_PRODUCTOPS_COMPONENT,

    /**
     * 部署 ProductOps V2 组件
     */
    DEPLOY_IA_V2_PRODUCTOPS_COMPONENT,

    /**
     * 构建 ProductOps 组件
     */
    BUILD_IA_PRODUCTOPS_COMPONENT,

    /**
     * 部署 ProductOps 组件
     */
    DEPLOY_IA_PRODUCTOPS_COMPONENT,

    /**
     * 构建 tianji ProductOps 组件
     */
    BUILD_IA_TIANJI_PRODUCTOPS_COMPONENT,

    /**
     * 部署 tianji ProductOps 组件
     */
    DEPLOY_IA_TIANJI_PRODUCTOPS_COMPONENT,

    /**
     * 构建 AppMeta 组件
     */
    BUILD_IA_APP_META_COMPONENT,

    /**
     * 构建 Development Meta 组件
     */
    BUILD_IA_DEVELOPMENT_META_COMPONENT,

    /**
     * 构建 AppBinding 组件
     */
    BUILD_IA_APP_BINDING_COMPONENT,

    /**
     * 部署 AppMeta 组件
     */
    DEPLOY_IA_APP_META_COMPONENT,

    /**
     * 构建 AbmOperatorTvd 组件
     */
    BUILD_ABM_OPERATOR_TVD_COMPONENT,

    /**
     * 部署 AbmOperatorTvd 组件
     */
    DEPLOY_ABM_OPERATOR_TVD_COMPONENT,

    /**
     * 部署 DevelopmentMeta 组件
     */
    DEPLOY_IA_DEVELOPMENT_META_COMPONENT,

    /**
     * 部署 AppBinding 组件
     */
    DEPLOY_IA_APP_BINDING_COMPONENT,

    /**
     * Trait
     */
    TRAIT,

    /**
     * V2: 组件类型
     */
    COMPONENT,

    /**
     * V2: 组件构建
     */
    COMPONENT_BUILD,

    /**
     * V2: 组件部署
     */
    COMPONENT_DEPLOY,

    /**
     * V2: 组件销毁
     */
    COMPONENT_DESTROY,

    /**
     * V2: 组件 Watch (Kubernetes Informer)
     */
    COMPONENT_WATCH_KUBERNETES_INFORMER,

    /**
     * V2: 组件 Watch (CRON)
     */
    COMPONENT_WATCH_CRON,

    /**
     * V2: Workflow
     */
    WORKFLOW,

    /**
     * V2: Policy
     */
    POLICY
}

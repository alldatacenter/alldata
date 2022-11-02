package com.alibaba.tesla.appmanager.common.constants;

/**
 * Component 部署单 Variable Key
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class ComponentFlowVariableKey {

    /**
     * 部署单 ID (Component 层面)
     */
    public static final String DEPLOY_ID = "DEPLOY_ID";

    /**
     * 部署单 ID (App 层面）
     */
    public static final String DEPLOY_APP_ID = "DEPLOY_APP_ID";

    /**
     * 应用 ID
     */
    public static final String APP_ID = "APP_ID";

    /**
     * 部署目标 Cluster
     */
    public static final String CLUSTER_ID = "CLUSTER_ID";

    /**
     * 部署目标 Namespace
     */
    public static final String NAMESPACE_ID = "NAMESPACE_ID";

    /**
     * 部署目标 Stage
     */
    public static final String STAGE_ID = "STAGE_ID";

    /**
     * Component 包下载 URL 地址
     */
    public static final String COMPONENT_PACKAGE_URL = "COMPONENT_PACKAGE_URL";

    /**
     * Component 包类型
     */
    public static final String COMPONENT_TYPE = "COMPONENT_TYPE";

    /**
     * Component 标识名称
     */
    public static final String COMPONENT_NAME = "COMPONENT_NAME";

    /**
     * Component Schema
     */
    public static final String COMPONENT_SCHEMA = "COMPONENT_SCHEMA";

    /**
     * Component Options (DeployAppSchema.SpecComponent)
     */
    public static final String COMPONENT_OPTIONS = "COMPONENT_OPTIONS";

    /**
     * 是否在本地集群中
     */
    public static final String IN_LOCAL_CLUSTER = "IN_LOCAL_CLUSTER";

    /**
     * Docker Daemon Address
     */
    public static final String DOCKER_DAEMON = "DOCKER_DAEMON";
}

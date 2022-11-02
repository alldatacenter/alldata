package com.alibaba.tesla.appmanager.common.enums;

/**
 * DAG 类型枚举
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum DagTypeEnum {

    /**
     * 部署 AppPackage
     */
    DEPLOY_APP,

    /**
     * 部署 ComponentPackage
     */
    DEPLOY_COMPONENT,

    /**
     * Addon Instance 申请任务
     */
    APPLY_ADDON_INSTANCE_TASK,

    /**
     * 应用包打包
     */
    PACK_APP_PACKAGE,

    /**
     * 应用包解包
     */
    UNPACK_APP_PACKAGE,

    /**
     * 未知类型
     */
    UNKNOWN;
}

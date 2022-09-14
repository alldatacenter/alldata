package com.alibaba.tesla.appmanager.common.enums;

/**
 * AppPackage 部署单状态
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum AppPackageTaskStatusEnum {

    /**
     * 部署单已创建
     */
    CREATED,

    /**
     * 组件打包中
     */
    COM_PACK_RUN,

    /**
     * 应用打包中
     */
    APP_PACK_RUN,


    /**
     * 成功
     */
    SUCCESS,

    /**
     * 失败
     */
    FAILURE;
}

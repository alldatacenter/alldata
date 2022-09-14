package com.alibaba.tesla.appmanager.common.enums;

/**
 * Addon 类型
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum AddonTypeEnum {
    /**
     * 内置服务插件
     */
    INTERNAL,

    /**
     * 第三方服务插件
     */
    RESOURCE,

    /**
     * 运维特性类 Addon
     */
    TRAIT,

    /**
     * 自定义 Addon
     */
    CUSTOM;
}

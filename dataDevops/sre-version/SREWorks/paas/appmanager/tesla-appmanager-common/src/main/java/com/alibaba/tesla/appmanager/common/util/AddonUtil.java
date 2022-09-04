package com.alibaba.tesla.appmanager.common.util;

import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;

/**
 * 附加组件工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class AddonUtil {

    public static final String TRAIT_PREFIX = "TRAIT@";

    /**
     * traitName -> addonId
     *
     * @param traitName 运维特性名称 -> 附加组件 ID
     * @return addonId
     */
    public static String traitToAddonId(String traitName) {
        return TRAIT_PREFIX + traitName;
    }

    /**
     * 组合 addonId + addonName 为一个字符串
     *
     * @param addonId   addonId
     * @param addonName addonName
     * @return String
     */
    public static String combineComponentName(String addonId, String addonName) {
        return addonId + "@" + addonName;
    }

    /**
     * 组合 Addon Key
     *
     * @param componentType 组件类型
     * @param addonId       Addon ID
     * @return Addon Key
     */
    public static String combineAddonKey(ComponentTypeEnum componentType, String addonId) {
        return String.format("%s-%s", componentType, addonId);
    }
}

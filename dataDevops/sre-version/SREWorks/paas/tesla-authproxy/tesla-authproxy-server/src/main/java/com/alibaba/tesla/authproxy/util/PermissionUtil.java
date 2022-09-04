package com.alibaba.tesla.authproxy.util;

public class PermissionUtil {

    /**
     * 获取部门对应的 user id
     *
     * @param depId 部门 ID
     * @return user id
     */
    public static String getDepUserId(String depId) {
        return "depid::" + depId;
    }

    /**
     * 获取指定应用的 guest 角色
     *
     * @param appId 应用 ID
     * @return 实际 guest 角色 ID
     */
    public static String getGuestRole(String appId) {
        return String.format("%s:guest", appId);
    }
}

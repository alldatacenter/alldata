package com.alibaba.tesla.appmanager.auth.util;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;

/**
 * 权限工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PermissionUtil {

    /**
     * 生成标准权限点
     *
     * @param type  类型
     * @param value 值
     * @param scope 范围
     * @return String
     */
    public static String generate(String type, String value, String scope) {
        return String.format("appmanager:%s:%s:%s", type, value, scope);
    }

    /**
     * 从 Permission 中解析出实际的 value 并返回
     *
     * @param permission 权限点
     * @return 解析后的 value
     */
    public static String extract(String permission) {
        String[] array = permission.split(":", 4);
        if (array.length < 4) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid permission " + permission);
        }
        return array[2];
    }
}

package com.alibaba.tesla.appmanager.common.util;

import java.util.Objects;

/**
 * @author qianmo.zm@alibaba-inc.com
 * @date 2020/10/27.
 */
public class CommonUtil {
    public static <T> T getValue(T value, T defaultValue) {
        return Objects.nonNull(value) ? value : defaultValue;
    }
}

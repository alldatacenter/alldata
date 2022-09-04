package com.alibaba.tesla.tkgone.server.common;

import org.apache.commons.lang3.StringUtils;

/**
 * 一些检查工具
 *
 * @author feiquan
 */
public class Assert {
    /**
     * 判断参数不能为空
     * @param argument
     * @param value
     */
    public static void notEmpty(String argument, String value) {
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException(argument + " cannot be empty");
        }
    }

    /**
     * 判断参数不能为空
     * @param argument
     * @param value
     */
    public static void notEmpty(String argument, Object[] value) {
        if (value == null || value.length == 0) {
            throw new IllegalArgumentException(argument + " cannot be empty");
        }
    }

    /**
     * 判断参数必须大于等于某个值
     * @param argument
     * @param value
     * @param condition
     */
    public static void greaterEquals(String argument, int value, int condition) {
        if (value < condition) {
            throw new IllegalArgumentException(argument + "(" + value + ")" + " must greater or equal to " + condition);
        }
    }

    /**
     * 判断参数必须大于某个值
     * @param argument
     * @param value
     * @param condition
     */
    public static void greater(String argument, int value, int condition) {
        if (value <= condition) {
            throw new IllegalArgumentException(argument + "(" + value + ")" + " must greater to " + condition);
        }
    }

    /**
     * 判断参数不能为null
     * @param argument
     * @param value
     */
    public static void notNull(String argument, Object value) {
        if (value == null) {
            throw new IllegalArgumentException(argument + " cannot be null");
        }
    }
}

package com.alibaba.tesla.authproxy.util;

/**
 * 字符串工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class StringUtil {

    /**
     * 判断给定字符串是否为空，可传入 Object
     *
     * @param str 需要判定的字符串
     * @return true or false
     */
    public static boolean isEmpty(Object str) {
        return (str == null || "".equals(str));
    }

    /**
     * 根据指定字符进行 trim 操作
     */
    public static String trim(String value, Character c) {
        int len = value.length();
        int st = 0;
        char[] val = value.toCharArray();

        while ((st < len) && (val[st] <= c)) {
            st++;
        }
        while ((st < len) && (val[len - 1] <= c)) {
            len--;
        }
        return ((st > 0) || (len < value.length())) ? value.substring(st, len) : value;
    }

}

package com.alibaba.sreworks.job.utils;

/**
 * @author jinghua.yjh
 */
public class StringUtil {

    public static boolean isEmpty(String string) {
        return string == null || "".equals(string) || "null".equals(string.toLowerCase());
    }

    public static Long toLong(String string) {
        return string == null ? null : Long.parseLong(string);
    }

    public static String toLowerCaseFirstOne(String s) {
        if (Character.isLowerCase(s.charAt(0))) {
            return s;
        } else {
            return Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }
    }
}

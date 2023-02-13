package com.alibaba.sreworks.domain.utils;

public class AppUtil {

    public static String appmanagerId(Long id) {
        return "sreworks" + id;
    }

    public static Long appId(String appmanagerId) {
        return Long.valueOf(appmanagerId.replace("sreworks", ""));
    }

}

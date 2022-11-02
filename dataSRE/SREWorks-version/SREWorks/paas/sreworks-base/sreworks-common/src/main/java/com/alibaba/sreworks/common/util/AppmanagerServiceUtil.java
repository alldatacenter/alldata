package com.alibaba.sreworks.common.util;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jinghua.yjh
 */
@Slf4j
public class AppmanagerServiceUtil {

    public static String getEndpoint() {
        return System.getenv("APPMANAGER_ENDPOINT");
    }

}

package com.alibaba.tesla.appmanager.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * @author QianMo
 * @date 2020/07/10.
 */
@Slf4j
public class ResourcesUtil {
    private static String url;

    public static String getURL() {
        if (StringUtils.isNotEmpty(url)) {
            return url;
        }
        File file = new File(".");
        if (file.exists()) {
            url = file.getAbsolutePath();
            log.info(">>>resourcesUtil|当前目录={}", url);
            return url;
        }

        return "";
    }
}

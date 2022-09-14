package com.alibaba.tesla.appmanager.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 环境工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class EnvUtil {

    /**
     * 返回当前是否为 SREWORKS 环境
     *
     * @return true or false
     */
    public static boolean isSreworks() {
        return "sreworks".equals(System.getenv("K8S_NAMESPACE"));
    }

    /**
     * 返回当前系统的默认 Namespace ID
     *
     * @return string
     */
    public static String defaultNamespaceId() {
        String result = System.getenv("DEFAULT_NAMESPACE_ID");
        if (StringUtils.isEmpty(result)) {
            if (isSreworks()) {
                return "sreworks";
            } else {
                return "default";
            }
        }
        return result;
    }

    /**
     * 返回当前系统的默认 Stage ID
     *
     * @return string
     */
    public static String defaultStageId() {
        String result = System.getenv("DEFAULT_STAGE_ID");
        if (StringUtils.isEmpty(result)) {
            if (isSreworks()) {
                return "dev";
            } else {
                return "pre";
            }
        }
        return result;
    }
}

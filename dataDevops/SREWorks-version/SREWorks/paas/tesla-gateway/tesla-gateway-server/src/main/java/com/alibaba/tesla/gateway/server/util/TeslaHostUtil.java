package com.alibaba.tesla.gateway.server.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class TeslaHostUtil {
    private static final List<String> TESLA_HOSTS = Arrays.asList("tesla.alibaba-inc.com", "tesladaily.alibaba-inc.com", "tesla-pre.alibaba-inc.com");

    /**
     * 是否属于主站域名
     * @param host
     * @return
     */
    public static boolean inTeslaHost(String referer) {
        //if (StringUtils.isBlank(referer)) {
        //    return false;
        //}
        //String[] firstRefererValues = referer.split("//");
        //if (firstRefererValues.length > 1) {
        //    return TESLA_HOSTS.contains(firstRefererValues[1].split("/")[0]);
        //}
        //return false;
        return true;
    }

}

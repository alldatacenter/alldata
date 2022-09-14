package com.alibaba.tesla.appmanager.common.util;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * 实例 ID 工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class InstanceIdUtil {

    /**
     * 返回一个新构造的 app instance id
     *
     * @return string
     */
    public static String genAppInstanceId(String appId, String clusterId, String namespaceId, String stageId) {
        if (StringUtils.isEmpty(clusterId)) {
            clusterId = "";
        }
        if (StringUtils.isEmpty(namespaceId)) {
            namespaceId = "";
        }
        if (StringUtils.isEmpty(stageId)) {
            stageId = "";
        }
        String identifier = String.format("%s-%s-%s-%s", appId, clusterId, namespaceId, stageId);
        return "app-" + StringUtil.md5sum(identifier);
    }

    /**
     * 返回一个新构造的 component instance id
     *
     * @return string
     */
    public static String genComponentInstanceId() {
        return "component-" + suffix();
    }

    /**
     * 返回一个新构造的 trait instance id
     *
     * @return string
     */
    public static String genTraitInstanceId() {
        return "trait-" + suffix();
    }

    /**
     * 返回一个随机的 UUID 字符串作为后缀
     *
     * @return uuid string
     */
    private static String suffix() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}

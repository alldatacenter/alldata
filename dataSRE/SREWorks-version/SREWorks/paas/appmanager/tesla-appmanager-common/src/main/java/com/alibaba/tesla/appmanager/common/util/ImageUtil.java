package com.alibaba.tesla.appmanager.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * 镜像工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class ImageUtil {

    /**
     * 生成当前环境中的 :latest 的镜像名称
     *
     * @param image 原始 image tag
     * @return 新的镜像名称
     */
    public static String generateLatestImage(String registry, String namespace, String image) {
        if (StringUtils.isEmpty(namespace)) {
            namespace = "abm";
        }
        String result = image
                .replace("abm.io/abm/", registry + "/" + namespace + "/")
                .replace("abm.io/abm-aone/", registry + "/" + namespace + "/");

        // 针对 ABM 专有云输出的定制修改镜像方式
        String cloudType = System.getenv("CLOUD_TYPE");
        if ("ApsaraStack".equals(cloudType) || "ApsaraStackAgility".equals(cloudType)) {
            result = result
                    .replace("reg.docker.alibaba-inc.com/abm-appmanager-x86/", registry + "/" + namespace + "/")
                    .replace("reg.docker.alibaba-inc.com/abm-appmanager-arm/", registry + "/" + namespace + "/")
                    .replace("reg.docker.alibaba-inc.com/abm-appmanager-sw6b/", registry + "/" + namespace + "/")
                    .replace("reg.docker.alibaba-inc.com/abm-private-x86/", registry + "/" + namespace + "/")
                    .replace("reg.docker.alibaba-inc.com/abm-private-arm/", registry + "/" + namespace + "/")
                    .replace("reg.docker.alibaba-inc.com/abm-private-sw6b/", registry + "/" + namespace + "/")
                    .replace("-x86-", "-")
                    .replace("-arm-", "-")
                    .replace("-sw6b-", "-")
                    .replace("-noarch-", "-");
            String splitIdentifier = "/" + namespace + "/";
            // 写死的规则
            String envName = result.substring(result.indexOf(splitIdentifier) + splitIdentifier.length(),
                    result.lastIndexOf(":")).toUpperCase().replaceAll("-", "_") + "_IMAGE";
            String envValue = System.getenv(envName);
            if (StringUtils.isNotEmpty(envValue)) {
                log.info("find envName {} in system, replace image|value={}|image={}", envName, envValue, image);
                return envValue;
            } else {
                log.info("cannot find envName {} in system, cannot replace image, skip|image={}|result={}",
                        envName, image, result);
                return result;
            }
        }
        return result;
    }

    /**
     * 获取 image 的 tag
     *
     * @param image 镜像名称 (reg.docker.alibaba-inc.com/abm/xxxx:xxxx)
     * @return tag
     */
    public static String getImageTag(String image) {
        return image.substring(image.lastIndexOf(":") + 1);
    }

    /**
     * 替换指定 image 中的 tag 为 newTag 对应的值
     *
     * @param image  镜像名称 (reg.docker.alibaba-inc.com/abm/xxxx:xxxx)
     * @param newTag 新的 tag 地址
     * @return new image name
     */
    public static String replaceImageTag(String image, String newTag) {
        return String.format("%s:%s", image.substring(0, image.lastIndexOf(":")), newTag);
    }

    /**
     * 获取实际 image 的保存相对位置
     *
     * @param appId         应用 ID
     * @param componentName component name
     * @param containerName container name
     * @return 相对位置路径
     */
    public static String getImagePath(String appId, String componentName, String containerName) {
        return String.format("%s-%s-%s.tar", appId, componentName, containerName);
    }

    /**
     * 获取随机 tag
     *
     * @return random tag
     */
    public static String randomTag() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}

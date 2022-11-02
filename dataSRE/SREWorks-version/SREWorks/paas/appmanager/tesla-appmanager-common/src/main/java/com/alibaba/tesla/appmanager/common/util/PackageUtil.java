package com.alibaba.tesla.appmanager.common.util;

import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;

import org.apache.commons.lang3.StringUtils;

/**
 * 包工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PackageUtil {

    /**
     * 生成 component package 的远端路径
     *
     * @param appId   应用 ID
     * @param type    类型
     * @param name    名称
     * @param version 版本
     * @return
     */
    public static String buildComponentPackageRemotePath(String appId, String type, String name, String version) {
        return String.format("components/%s/%s/%s/%s.zip", appId, type, name, version);
    }

    public static String buildKanikoBuildRemotePath(
            String appId, String type, String componentName, String containerName, String version) {
        return String.format("components/kaniko/%s/%s/%s/%s/%s.tar.gz", appId, type, componentName, containerName, version);
    }

    /**
     * 生成 app package 的远端路径
     *
     * @param appId   应用 ID
     * @param version 应用包版本号
     * @return
     */
    public static String buildAppPackageRemotePath(String appId, String version) {
        return String.format("apps/%s/%s.zip", appId, version);
    }

    /**
     * 生成 PackagePath 标准路径字符串 (for AppPackage)
     *
     * @param bucketName Bucket 名称
     * @param appId      应用 ID
     * @param version    应用包版本号
     * @return
     */
    public static String buildAppPackagePath(String bucketName, String appId, String version) {
        return String.format("%s/%s", bucketName, buildAppPackageRemotePath(appId, version));
    }

    /**
     * 生成 PackagePath 标准路径字符串 (for ComponentPackage)
     *
     * @param bucketName Bucket 名称
     * @param appId      应用 ID
     * @param type       组件类型
     * @param name       组件名称
     * @param version    应用包版本号
     * @return
     */
    public static String buildComponentPackagePath(
            String bucketName, String appId, String type, String name, String version) {
        return String.format("%s/%s", bucketName, buildComponentPackageRemotePath(appId, type, name, version));
    }

    /**
     * 获取 Component Package 的元信息 Yaml 字符串
     *
     * @param packagePath component package zip 包本地绝对路径
     * @return yaml string
     */
    public static String getComponentPackageMeta(String packagePath) {
        return ZipUtil.getZipSpecifiedFile(packagePath, "meta.yaml");
    }

    public static String fullVersion(String oldVersion, String newVersion) {
        if (StringUtils.isNotBlank(oldVersion)) {
            if (StringUtils.equals(newVersion, DefaultConstant.AUTO_VERSION)) {
                return VersionUtil.buildVersion(VersionUtil.buildNextPatch(oldVersion));
            } else {
                return VersionUtil.buildVersion(oldVersion, newVersion);
            }
        } else {
            if (StringUtils.equals(newVersion, DefaultConstant.AUTO_VERSION)) {
                return VersionUtil.buildVersion(DefaultConstant.INIT_VERSION);
            } else {
                return VersionUtil.buildVersion(newVersion);
            }
        }
    }
}

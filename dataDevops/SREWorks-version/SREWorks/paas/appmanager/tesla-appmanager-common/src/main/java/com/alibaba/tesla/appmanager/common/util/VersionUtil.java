package com.alibaba.tesla.appmanager.common.util;

import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.SemverException;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * 版本工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class VersionUtil {

    private static final String SIMPLE_DATE_FORMAT = "yyyyMMddHHmmss";

    /**
     * 版本合法性检查
     *
     * @param oldVersionStr 旧版本
     * @param newVersionStr 新版本, 新版本可能是 null，此时直接使用旧版本自增
     * @return 新版本号 (包含 build 信息)
     */
    public static String buildVersion(String oldVersionStr, String newVersionStr) {
        Semver oldVersion = new Semver(oldVersionStr);
        Semver newVersion;
        if (StringUtils.isEmpty(newVersionStr)) {
            newVersionStr = buildNextPatch(oldVersionStr);
        }
        try {
            newVersion = new Semver(newVersionStr);
        } catch (SemverException e) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("invalid format for version %s", newVersionStr));
        }
        if (!newVersion.isGreaterThan(oldVersion)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("the version %s provided in the current request cannot be used as the " +
                            "next version of version %s", newVersion.getValue(), oldVersion.getValue()));
        }
        return buildVersion(newVersionStr);
    }

    public static int compareTo(String version1, String version2) {
        if (StringUtils.isEmpty(version1) || StringUtils.isEmpty(version2)) {
            return StringUtils.equals(version1, version2) ? 0 : 1;
        }

        return new Semver(version1).compareTo(new Semver(version2));
    }

    /**
     * 创建一个空版本号，符合默认要求
     *
     * @return 空版本号 (起始值)
     */
    public static String buildNextPatch() {
        return buildNextPatch("");
    }

    /**
     * 创建指定 version 的下一个版本
     *
     * @param oldVersionStr version
     * @return 下一个版本号
     */
    public static String buildNextPatch(String oldVersionStr) {
        if (StringUtils.isEmpty(oldVersionStr)) {
            oldVersionStr = DefaultConstant.INIT_VERSION;
        }

        Semver version;
        try {
            version = new Semver(oldVersionStr);
        } catch (SemverException e) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("invalid format for version %s", oldVersionStr));
        }

        if (version.withClearedBuild().getMinor() >= 9) {
            return version.withClearedBuild().nextMajor().getValue();
        } else if (version.withClearedBuild().getPatch() >= 9) {
            return version.withClearedBuild().nextMinor().getValue();
        } else {
            return version.withClearedBuild().nextPatch().getValue();
        }
    }

    /**
     * 生成新版本号
     *
     * @param versionStr 版本号
     * @return 新版本号（包含 build 信息）
     */
    public static String buildVersion(String versionStr) {
        check(versionStr);
        Semver version;
        try {
            version = new Semver(versionStr);
        } catch (SemverException e) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("invalid format for version %s, the legal version format is x.x.x", versionStr));
        }
        return version.withClearedBuild().withBuild(currentDatetime()).getValue();
    }

    public static String clear(String versionStr) {
        check(versionStr);
        Semver version;
        try {
            version = new Semver(versionStr);
        } catch (SemverException e) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("invalid format for version %s, the legal version format is x.x.x", versionStr));
        }
        return version.withClearedBuild().getValue();
    }

    private static void check(String versionStr) {
        if (StringUtils.isEmpty(versionStr)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    "you need to provide the version number when you first create it");
        }
    }

    /**
     * 返回当前时间字符串
     *
     * @return example: 20200916170600
     */
    private static String currentDatetime() {
        Random rnd = new Random();
        return (new SimpleDateFormat(SIMPLE_DATE_FORMAT).format(new Date())) +
                String.format("%d", 100000 + rnd.nextInt(900000));
    }
}

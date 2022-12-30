package com.alibaba.tesla.appmanager.common.enums;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class AppMsDeployType {

    public static final String SHARE = "share";

    public static final String EXCLUSIVE = "exclusive";

    public static String getDeployType(boolean share) {
        return share ? SHARE : EXCLUSIVE;
    }
}

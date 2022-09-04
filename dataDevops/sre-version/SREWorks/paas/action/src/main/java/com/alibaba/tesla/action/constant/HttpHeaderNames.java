package com.alibaba.tesla.action.constant;

/**
 * 兼容已有系统的 X header 定义
 *
 * @author dongdong.ldd@alibaba-inc.com
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class HttpHeaderNames {

    /**
     * 每个请求唯一的 trace id
     */
    public static final String X_TRACE_ID = "X-Traceid";

    /**
     * 用户名信息 (一二代鉴权通用)
     */
    public static final String X_AUTH_USER = "x-auth-user";

    /**
     * 用户工号信息
     */
    public static final String X_EMPL_ID = "x-empid";

    /**
     * 用户邮箱信息
     */
    public static final String X_EMAIL_ADDR = "x-email-addr";

    /**
     * 密码 (一代鉴权)
     */
    public static final String X_AUTH_USER_PASSWD = "x-auth-passwd";

    /**
     * Auth App (一二代鉴权通用)
     */
    public static final String X_AUTH_APP = "x-auth-app";

    /**
     * Auth Key (一代鉴权)
     */
    public static final String X_AUTH_APP_KEY = "x-auth-key";

    /**
     * Auth UserId (二代鉴权)
     */
    public static final String X_AUTH_USERID = "x-auth-userid";

    /**
     * 应用唯一标识
     */
    public static final String X_BIZ_APP = "X-Biz-App";

    /**
     * headers added by Nginx proxy
     */
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String ORIG_CLIENT_IP = "ORIG_CLIENT_IP";
    public static final String CLIENT_IP = "Client-IP";
}

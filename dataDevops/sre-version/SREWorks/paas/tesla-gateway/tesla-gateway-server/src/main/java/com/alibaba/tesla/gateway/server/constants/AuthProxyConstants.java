package com.alibaba.tesla.gateway.server.constants;

/**
 * @Date 2019-09-05 10:00
 * @Author cdx
 **/
public class AuthProxyConstants {

    /**
     * bucCookie标识：teslaJwt鉴权token
     */
    public static final String COOKIE_SSO_LOGIN_TOKEN = "bcc_sso_token";

    /**
     * Header标识：jwt
     */
    public static final String HEADER_NAME_TOKEN = "Authorization";
    public static final String HTTP_BASIC_AUTH_HEADER = "Authorization";

    /**
     * 请求头名称
     */
    public static final String HEADER_NAME_APP = "x-auth-app";
    public static final String HEADER_NAME_XBIZAPP = "X-Biz-App";
    public static final String HEADER_NAME_KEY = "x-auth-key";
    public static final String HEADER_NAME_USER = "x-auth-user";
    public static final String HEADER_NAME_USER_ID = "x-auth-userid";
    public static final String HEADER_NAME_EMP_ID = "x-empid";
    public static final String HEADER_NAME_PASSWD = "x-auth-passwd";
    public static final String HEADER_NAME_LOCAL = "X-Locale";

    /**
     * 原auth_proxy获取并处理的header(不再兼容)
     */
    public static final String V1_HEADER_USER = "X-ACCESS-USER";
    public static final String V1_HEADER_EMPID = "X-ACCESS-EMPID";
    public static final String V1_HEADER_KEY = "X-ACCESS-KEY";

    /**
     * 原auth_proxy的存储userInfo的cookie
     */
    public static final String V1_COOKIE_NAME = "redcoast-userinfo";

    /**
     * 原auth_proxy通过cookie传递给后端的header
     */
    public static final String V1_HEADER_EMAIL = "x-email-addr";
    public static final String V1_HEADER_BUC_ID = "x-buc-id";

    /**
     * 专有云环境下的国际化语言cookie名称
     */
    public static final String COOKIE_LANG = "aliyun_lang";
    public static final String COOKIE_COUNTRY = "aliyun_country";

}

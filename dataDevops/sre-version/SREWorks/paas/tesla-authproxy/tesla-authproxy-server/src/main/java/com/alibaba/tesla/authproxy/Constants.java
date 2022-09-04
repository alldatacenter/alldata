package com.alibaba.tesla.authproxy;

/**
 * <p>Title: AuthConstants.java<／p>
 * <p>Description: 常量类<／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年4月9日
 */
public class Constants {
    /**
     * 本地 IP 地址
     */
    public static final String LOCAL_HOST = "127.0.0.1";

    /**
     * Cookie标识：登录用户ID
     */
    public static final String COOKIE_AAS_LOGIN_USER_ID = "login_aliyunid_pk";
    public static final String COOKIE_SSO_LOGIN_USER_ID = "sso_loginuser_id";
    public static final String COOKIE_DATABASE_LOGIN_USER_ID = "tesla_user_id";

    /**
     * Cookie标识：aas登录ticket
     */
    public static final String COOKIE_AAS_LOGIN_ALIYUNID = "login_aliyunid";
    public static final String COOKIE_AAS_LOGIN_TICKET = "login_aliyunid_ticket";

    /**
     * Cookie标识：aliyun相关token
     */
    public static final String COOKIE_ALIYUN_ACCESS_TOKEN = "aliyun_access_token";
    public static final String COOKIE_ALIYUN_REFRESH_TOKEN = "aliyun_refresh_token";
    public static final String COOKIE_ALIYUN_EXPIRES_AT = "aliyun_expires_at";

    /**
     * Cookie标识：sso登录ticket
     */
    public static final String COOKIE_SSO_LOGIN_TOKEN = "login_sso_token";

    /**
     * Cookie标识：DataBase登录ticket
     */
    public static final String COOKIE_DATABASE_LOGIN_TOKEN = "tesla_token";

    /**
     * 默认租户
     */
    public static final String DEFAULT_TENANT_ID = "alibaba";

    /**
     * 默认访客角色
     */
    public static final String DEFAULT_GUEST_ROLE = "guest";

    /**
     * 默认语言
     */
    public static final String DEFAULT_LOCALE = "zh_CN";

    /**
     * 默认语言匹配表达式
     */
    public static final String DEFAULT_LOCALE_REGEX = "^(zh_CN|zh_HK|en_US|zh_MO|)$";

    public static final int DEFAULT_COOKIE_TIME = 7 * 24 * 60 * 60 * 1000;

    /**
     * Cookie标识：语言
     */
    public static final String ALIYUN_COOKIE_LANG = "aliyun_lang";
    public static final String ALIYUN_COOKIE_TERRITORY = "aliyun_territory";
    public static final String COOKIE_LANG = "lang";
    public static final String COOKIE_COUNTRY = "country";

    /**
     * 服务端错误
     */
    public static final String RES_MSG_SERVERERROR = "权限服务异常";
    public static final String RES_MSG_SERVERERROR_NOTSAFE = "回调地址不安全";
    public static final String RES_MSG_REFERER_URL_WRONG_ENCODING = "Referer URL 编码不正确";

    /**
     * 请求源地址
     */
    public static final String REFERER_URL = "refererUrl";

    /**
     * 配置项常量
     */
    public static final String CONFIG_PRIVATE_ACCOUNT_VALIDATION = "private_account_validation";
    public static final String CONFIG_PRIVATE_ACCOUNT_VALIDATION_PASSWORD = "password";
    public static final String CONFIG_PRIVATE_ACCOUNT_VALIDATION_PASSWORD_MOBILE = "password_mobile";
    public static final String CONFIG_PRIVATE_SMS_ENDPOINT = "private_sms_endpoint";
    public static final String CONFIG_PRIVATE_SMS_TOKEN = "private_sms_token";

    /**
     * 权限常量
     */
    public static final String PERMISSION_GATEWAY = "gateway";

    /**
     * 第三方组件名称常量
     */
    public static final String THIRD_PARTY_ACS = "ACS";
    public static final String THIRD_PARTY_AK = "AK";
    public static final String THIRD_PARTY_AAS = "AAS";
    public static final String THIRD_PARTY_CALLBACK = "CALLBACK";
    public static final String THIRD_PARTY_OAM = "OAM";
    public static final String THIRD_PARTY_SMS = "SMS";

    /**
     * OP Log Action 常量
     */
    public static final String OP_PASSWORD_CHANGE = "password_change";

    /**
     * OAM 权限前缀
     */
    public static final String PERMISSION_PREFIX_API = "bcc_api_";
    public static final String ABM_ROLE_PREFIX_API = "bcc_role_";

    /**
     * Request 传递常量
     */
    public static final String REQUEST_ATTR_USER = "authUser";

    /**
     * 当前环境常量
     */
    public static final String ENVIRONMENT_INTERNAL = "Internal";
    public static final String ENVIRONMENT_V3 = "ApsaraStack";
    public static final String ENVIRONMENT_DXZ = "DXZ";
    public static final String ENVIRONMENT_OXS = "OXS";
    public static final String ENVIRONMENT_STANDALONE = "Standalone";
    public static final String ENVIRONMENT_PAAS = "PaaS";

    /**
     * 控制与 expires_in 还剩 10 分钟的时候，获取新的 token
     */
    public static final long OAUTH2_EXPIRE_GAP = 600;

    /**
     * Millis -> secs 的转换
     */
    public static final long MILLS_SECS = 1000;
}

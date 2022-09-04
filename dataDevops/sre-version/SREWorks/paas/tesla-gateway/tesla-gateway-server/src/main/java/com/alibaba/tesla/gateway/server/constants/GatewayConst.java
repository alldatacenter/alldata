package com.alibaba.tesla.gateway.server.constants;

/**
 * @date 2019-08-26 17:00
 * @author cdx
 **/
public class GatewayConst {

    public static final String HEADER_NAME_TOKEN = "Authorization";

    public static final String HEADER_NAME_APP = "x-auth-app";
    public static final String HEADER_NAME_XBIZAPP = "X-Biz-App";
    public static final String HEADER_NAME_KEY = "x-auth-key";
    public static final String HEADER_NAME_USER = "x-auth-user";
    public static final String HEADER_NAME_USER_ID = "x-auth-userid";
    public static final String HEADER_NAME_EMP_ID = "x-empid";
    public static final String HEADER_NAME_PASSWD = "x-auth-passwd";
    public static final String HEADER_NAME_LOCAL = "X-Locale";

    public static final String X_TRACE_ID = "x-traceid";

    public static final String X_ENV_NAME = "X-ENV";

    public static final String TESLA_X_ENV_DAILY = "daily";

    public static final String TESLA_X_ENV_PRE = "pre";

    public static final String TESLA_X_ENV_PROD = "prod";

    /**
     * 网关日常路由ID
     */
    public static final String GATEWAY_DAILY_ROUTE_ID = "gateway-forward-env-daily";

    /**
     * 网关预发路由ID
     */
    public static final String GATEWAY_PRE_ROUTE_ID = "gateway-forward-env-pre";


    public static final String UNKNOWN = "unknown";

    public static final String TRACE_ID = "eagleeye-traceid";

    public static final String TESLA_GATEWAY_ACCESS = "tesla-gateway-access-number";

    public static final String ACCESS_ONCE = "1";

    public static final String AUTH_CHECK_RESULT = "tesla-auth-check-result";

    public static final String AUTH_CHECK_SUCCESS = "success";

    public static final String AUTH_CHECK_FAILED = "failed";

    public static final String ALIYUN_LOGIN_TICKET_NAME = "login_aliyunid_ticket";
}

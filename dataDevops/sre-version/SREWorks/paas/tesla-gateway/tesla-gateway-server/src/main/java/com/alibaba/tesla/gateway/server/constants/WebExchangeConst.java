package com.alibaba.tesla.gateway.server.constants;

/**
 * Web exchange 中存储的变量，统一在这里定义，防止冲突
 * 以 tesla_ 开头
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class WebExchangeConst {

    /**
     * 执行开始时间
     */
    public static final String EXEC_START_TIME = "tesla_exec_start_time";

    /**
     * 用户信息，empId|user name
     */
    public static final String AUTH_USER = "tesla_user";

    /**
     * APP id | app name
     */
    public static final String APP_ID = "tesla_app_id";

    public static final String REAL_REQUEST_URL = "tesla_real_request_url";

    public static final String TESLA_ORIGINAL_REQUEST_URL = "tesla_original_request_url";

    public static final String TESLA_IS_FORWARD_ENV = "tesla_is_forward_env";

    public static final String TESLA_AUTH_CHECK_TIME = "tesla_auth_check_cost_time";
}

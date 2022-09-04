package com.alibaba.tesla.gateway.server.util;

/**
 * 常量
 * @author tandong.td@alibaba-inc.com
 */
public class GatewayConstants {

    /**
     * 当前运行环境类型
     * Internal 集团内
     */
    public static final String ENVIRONMENT_INTERNAL = "Internal";
    public static final String ENVIRONMENT_OXS = "OXS";
    public static final String ENVIRONMENT_STANDALONE = "Standalone";
    public static final String ENVIRONMENT_V3 = "ApsaraStack";
    public static final String ENVIRONMENT_DXZ = "DXZ";

    public static final String ENV_LOCAL = "local";
    public static final String ENV_DAILY = "daily";
    public static final String ENV_PRE = "pre";
    public static final String ENV_PROD = "prod";

    /**
     * CONTEXT中存在的key名称
     * START_TIME：用于统计请求开始时间
     */
    public static final String START_TIME = "request_start_time";

    /**
     * 用于在 request 中设置的 timer 计时
     */
    public static final String ATTR_REQUEST_TIMER = "REQUEST_TIMER";

}

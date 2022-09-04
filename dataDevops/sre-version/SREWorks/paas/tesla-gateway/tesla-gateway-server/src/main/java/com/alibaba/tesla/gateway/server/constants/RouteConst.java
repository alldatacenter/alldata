package com.alibaba.tesla.gateway.server.constants;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class RouteConst {
    public static final String NOT_FOUND_PREFIX = "router not found, routeId=%s";

    public static final String DISCOVERY_SERVER_PREFIX = "lb://%s";

    /**
     * ref : https://yuque.antfin-inc.com/bdsre/tesla-gateway/standard
     * paas中台服务默认路由顺序
     */
    public static final int PAAS_ROUTE_DEFAULT_ORDER = 15;

    /**
     * faas 服务默认路由顺序
     */
    public static final int FAAS_ROUTE_DEFAULT_ORDER = 35;

    /**
     * 用户自定义路由顺序
     */
    public static final int APP_ROUTE_DEFAULT_ORDER = 55;
}

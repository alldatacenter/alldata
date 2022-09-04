package com.alibaba.tesla.gateway.server.util;

/**
 * 路由元数据的key
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class RouteMetaDataKeyConstants {

    /**
     * 鉴权忽略path
     */
    public static final String AUTH_IGNORE_PATHS = "authIgnorePaths";

    /**
     * 服务类型
     */
    public static final String SERVER_TYPE = "serverType";

    /**
     * 路由类型，PATH, HOST
     */
    public static final String ROUTE_TYPE = "routeType";

    /**
     * 前缀
     */
    public static final String STRIP_PREFIX = "stripPrefix";

    /**
     * 启用doc
     */
    public static final String ENABLED_DOC = "enabledDoc";

    /**
     * doc uri
     */
    public static final String DOC_URI = "docUri";

    /**
     *   启用鉴权失败重定向
     */
    public static final String AUTH_DIRECTION = "authDirection";

    /**
     * 黑名单配置
     */
    public static final String BLACK_LIST = "blackList";

}

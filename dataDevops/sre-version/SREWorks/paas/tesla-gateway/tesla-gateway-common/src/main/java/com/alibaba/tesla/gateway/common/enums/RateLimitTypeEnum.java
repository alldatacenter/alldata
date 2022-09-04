package com.alibaba.tesla.gateway.common.enums;

/**
 * 流量控制策略
 * @author qiuqiang.qq@alibaba-inc.com
 */
public enum RateLimitTypeEnum {

    /**
     * user id
     */
    USER_ID,

    /**
     * client id
     */
    CLIENT_ID,

    /**
     * 默认限流，只拿routeId来限流
     */
    ROUTE_ID,

    /**
     * 无限流
     */
    NO_LIMIT;

}

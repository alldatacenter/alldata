package com.alibaba.tesla.gateway.common.enums;

/**
 * 流量控制拼接参数
 * @author qiuqiang.qq@alibaba-inc.com
 */
public enum RateLimitParamEnum {

    /**
     * x-auth-app
     */
    AUTH_APP,

    /**
     * x-auth-user
     */
    AUTH_USER,

    /**
     * 请求机器IP
     */
    REMOTE_HOST_IP,

    /**
     * URL
     */
    URL;
}

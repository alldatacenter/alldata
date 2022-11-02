package com.alibaba.tesla.gateway.server.filter.global;

import org.springframework.core.Ordered;

/**
 * 全局过滤器管理
 * @author qiuqiang.qq@alibaba-inc.com
 */
public class GlobalFilterOrderManager {

    //自定义都从0开始


    public static final int LOG_GLOBAL_FILTER = Ordered.HIGHEST_PRECEDENCE;


    public static final int ENV_FORWARD_FILTER = -20;

    public static final int VIEW_SWITCH_FILTER = -4;

    public static final int AUTH_CHECK_FILTER = -5;

    public static final int AUTH_REDIRECT_FILTER = Ordered.HIGHEST_PRECEDENCE;

    public static final int BALK_LIST_FILTER = -3;

}


package com.alibaba.tesla.gateway.domain.req;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
public class RouteRateLimit implements Serializable {

    /**
     * 默认窗口时间
     */
    public static final Integer DEFAULT_INTERVAL = 1;

    /**
     * 默认请求次数
     */
    public static final Integer DEFAULT_REQUEST_COUNT = 100;

    /**
     * 类型 {@link com.alibaba.tesla.gateway.common.enums.RateLimitTypeEnum}
     */
    private String type;

    /**
     * 在刷新时间窗口中允许调用的次数
     */
    int limit;

}

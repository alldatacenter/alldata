package com.alibaba.tesla.gateway.server.domain;

import lombok.Data;

import java.util.Date;

/**
 * 和数据库中表对应
 * @author tandong
 * @Description:TODO
 * @date 2019/3/1 16:53
 */
@Data
public class RateLimit {

    long id;

    /**
     * 路由名称 zuul route's serviceId
     */
    String routeName;

    /**
     * 限流类型：如
     * ORIGIN
     * URL
     * USER
     */
    String types = "URL";

    /**
     * 刷新时间窗口（单位：秒），如1分钟只这限流请求10000次，那么 refreshInterval = 60
     */
    Long refreshInterval = 60L;

    /**
     * 在刷新时间窗口中允许调用的次数
     */
    Long limit = 600000L;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;
}

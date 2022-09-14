package com.alibaba.tesla.gateway.log.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
public class ExecLog implements Serializable {
    private static final long serialVersionUID = 3298170263365120364L;

    private String id;

    private String traceId;

    private String type;

    private String xEnv;

    private String routeId;

    private String httpMethod;

    private String originUri;

    private String rawQuery;

    private String headers;

    private String requestHost;

    private String originHost;

    private String targetUri;

    private String targetHost;

    private String httpCode;

    private String cost;

    private String sourceFlag;

    private String empId;

    private String createTime;

    private String startTime;

    private String authCostTime;
}

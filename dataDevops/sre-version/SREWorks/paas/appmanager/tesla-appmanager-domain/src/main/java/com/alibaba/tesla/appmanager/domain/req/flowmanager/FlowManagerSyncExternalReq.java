package com.alibaba.tesla.appmanager.domain.req.flowmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 同步 Flow Jar 到外部环境请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowManagerSyncExternalReq implements Serializable {

    private static final long serialVersionUID = -5422923655814125319L;

    /**
     * 目标代理 IP
     */
    private String proxyIp;

    /**
     * 目标代理端口
     */
    private int proxyPort;

    /**
     * 目标 Endpoint
     */
    private String endpoint;
}

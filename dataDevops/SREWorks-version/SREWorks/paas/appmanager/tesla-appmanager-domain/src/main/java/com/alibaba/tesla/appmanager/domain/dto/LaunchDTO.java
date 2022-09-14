package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaunchDTO {

    /**
     * 网关路由
     */
    private String gatewayRoute;

    /**
     * 网关路由优先级
     */
    private Integer gatewayRouteOrder;

    /**
     * 网关是否启用鉴权
     */
    private Boolean gatewayAuthEnabled;

    /**
     * 服务端口
     */
    private Integer servicePort;

    /**
     * 多个服务端口 80:7001,7001:7001
     */
    private String servicePorts;

    /**
     * Pod副本数量
     */
    private Integer replicas;

    /**
     * 时区
     */
    private String timezone;

    /**
     * 服务标签
     */
    private JSONObject serviceLabels;

    /**
     * Pod labels
     */
    private JSONObject podLabels;

    /**
     * Namespace资源限制
     */
    private JSONObject namespaceResourceLimit;


}

package org.dromara.cloudeon.controller.request;

import lombok.Data;

@Data
public class ModifyClusterInfoRequest {
    private Integer id;
    private Integer stackId;
    private String clusterName;
    private String clusterCode;
    private String kubeConfig;
}

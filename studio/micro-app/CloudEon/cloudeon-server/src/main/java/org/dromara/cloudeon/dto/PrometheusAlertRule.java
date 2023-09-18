package org.dromara.cloudeon.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PrometheusAlertRule implements Serializable {
    private String alertName;

    private Integer clusterId;

    private String alertExpr;

    private String serviceRoleName;

    private String alertLevel;

    private String alertAdvice;

    private Integer triggerDuration;
}

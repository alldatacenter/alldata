package com.elasticsearch.cloud.monitor.metric.common.metric;

import lombok.Data;

import java.io.Serializable;

/**
 * 指标对象
 *
 * @author: fangzong.lyj
 * @date: 2021/09/05 11:47
 */

@Data
public class MetricAdConfig implements Serializable {

    private String id;

    private String title;

    private String metricId;

    private Long ruleId;

    private Boolean enable;
}

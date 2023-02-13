package com.elasticsearch.cloud.monitor.metric.common.metric;

import lombok.Data;

import java.io.Serializable;

/**
 * 指标实例对象
 *
 * @author: fangzong.lyj
 * @date: 2021/09/05 11:47
 */

@Data
public class MetricInstance implements Serializable {

    private String id;

    private String name;

    private String metricId;

    private String metricName;

    private String indexPath;

    private String indexTags;
}

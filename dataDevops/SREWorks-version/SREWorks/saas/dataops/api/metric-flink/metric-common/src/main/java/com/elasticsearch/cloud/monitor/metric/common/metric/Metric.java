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
public class Metric implements Serializable {

    private String id;

    private String name;

    private String type;

    private String indexPath;

    private String tags;

    private String teamId;

    private String appId;
}

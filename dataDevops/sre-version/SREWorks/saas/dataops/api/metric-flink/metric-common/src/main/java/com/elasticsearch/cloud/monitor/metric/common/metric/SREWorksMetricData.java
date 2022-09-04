package com.elasticsearch.cloud.monitor.metric.common.metric;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.Objects;

/**
 * 指标数据对象
 *
 * @author: fangzong.lyj
 * @date: 2022/01/17 17:39
 */
@Data
public class SREWorksMetricData {
    private String uId;

    private Integer metricId;

//    private String metricName;

//    private String type;

    private JSONObject labels;

    private Long timestamp;

    private Long ts;

    private Float value;

    public boolean fieldValidCheck() {
        return Objects.nonNull(uId) && Objects.nonNull(metricId) && Objects.nonNull(timestamp) && Objects.nonNull(ts)
                && Objects.nonNull(value);
    }
}

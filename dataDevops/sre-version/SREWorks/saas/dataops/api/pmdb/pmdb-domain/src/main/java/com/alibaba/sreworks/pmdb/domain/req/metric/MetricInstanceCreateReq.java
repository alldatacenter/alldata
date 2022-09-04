package com.alibaba.sreworks.pmdb.domain.req.metric;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.springframework.util.CollectionUtils;

/**
 * 创建指标实例配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 19:34
 */
@ApiModel(value="指标实例配置")
public class MetricInstanceCreateReq extends  MetricInstanceBaseReq {
    @Override
    public Integer getMetricId() {
        Preconditions.checkArgument(metricId != null, "metric id must not be empty");
        return metricId;
    }

    @Override
    public JSONObject getLabels() {
        if (CollectionUtils.isEmpty(labels)) {
            labels = new JSONObject();
        }
        return labels;
    }
}

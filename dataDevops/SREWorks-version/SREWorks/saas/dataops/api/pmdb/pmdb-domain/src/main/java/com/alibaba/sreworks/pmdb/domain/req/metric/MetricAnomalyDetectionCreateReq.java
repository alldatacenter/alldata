package com.alibaba.sreworks.pmdb.domain.req.metric;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 创建指标异常检测配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 19:34
 */
@ApiModel(value="指标异常检测配置")
@Data
public class MetricAnomalyDetectionCreateReq extends MetricAnomalyDetectionBaseReq {

    @Override
    public String getTitle() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(title), "title must not be empty");
        return title;
    }

    @Override
    public Integer getRuleId() {
        Preconditions.checkArgument(ruleId != null, "rule id must not be empty");
        return ruleId;
    }

    @Override
    public Integer getMetricId() {
        Preconditions.checkArgument(metricId != null, "metric id must not be empty");
        return metricId;
    }

    @Override
    public Boolean getEnable() {
        if (enable == null) {
            enable = true;
        }
        return enable;
    }
}

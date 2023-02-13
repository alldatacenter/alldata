package com.alibaba.sreworks.pmdb.domain.req.metric;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;


/**
 * 创建指标配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 19:34
 */
@ApiModel(value="指标配置")
public class MetricCreateReq extends  MetricBaseReq {
    @Override
    public String getName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "metric name must not be empty");
        return name;
    }

    @Override
    public String getAlias() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(alias), "alias must not be empty");
        return alias;
    }

    @Override
    public String getType() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(type), "metric type path must not be empty");
//        Preconditions.checkArgument(MetricConstant.METRIC_TYPES.contains(type), "metric type is invalid");
        return type;
    }

    @Override
    public JSONObject getLabels() {
        if (CollectionUtils.isEmpty(labels)) {
            labels = new JSONObject();
        }
        return labels;
    }
}

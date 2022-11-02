package com.alibaba.sreworks.pmdb.domain.req.metric;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.pmdb.common.constant.Constant;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2022/01/17 20:17
 */
@ApiModel(value="指标数据")
@Data
public class MetricDataReq {
    @ApiModelProperty(value = "指标实例标签", example = "")
    JSONObject labels;

    @ApiModelProperty(value = "时间戳(毫秒)", required = true, example = "")
    Long timestamp;

    @ApiModelProperty(value = "数值", required = true, example = "0.0")
    Float value;

    public Long getTimestamp() {
        Preconditions.checkNotNull(timestamp, "时间戳[ts]需要提供");
        String tsStr = String.valueOf(timestamp);
        Preconditions.checkArgument(tsStr.length() == Constant.SECOND_TS_LENGTH || tsStr.length() == Constant.MILLISECOND_TS_LENGTH, "时间戳非法");
        if (tsStr.length() == Constant.SECOND_TS_LENGTH) {
            timestamp = timestamp * 1000;
        }
        return timestamp;
    }

    public Float getValue() {
        Preconditions.checkNotNull(value, "指标数值[value]需要提供");
        return value;
    }
}

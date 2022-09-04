package com.alibaba.sreworks.pmdb.domain.req.metric;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 指标实例配置基类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 19:34
 */
@ApiModel(value="指标实例配置")
@Data
public class MetricInstanceBaseReq {
    @ApiModelProperty(value = "指标ID", example = "1")
    Integer metricId;

    @ApiModelProperty(value = "指标tags", example = "k1=v")
    JSONObject labels;

    @ApiModelProperty(value = "实例说明", example = "xxx")
    String description;
}

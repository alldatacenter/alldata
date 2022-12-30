package com.alibaba.sreworks.pmdb.domain.req.metric;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 指标异常检测配置基类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 19:34
 */
@ApiModel(value="指标异常检测配置")
@Data
public class MetricAnomalyDetectionBaseReq {

    @ApiModelProperty(value = "配置名称", example = "xxx检测")
    String title;

    @ApiModelProperty(value = "规则ID", example = "10001")
    Integer ruleId;

    @ApiModelProperty(value = "指标ID", example = "1")
    Integer metricId;

    @ApiModelProperty(value = "是否生效", example = "true")
    Boolean enable;

    @ApiModelProperty(value = "创建人", example = "user1")
    String creator;

    @ApiModelProperty(value = "负责人", example = "user2")
    String owners;

    @ApiModelProperty(value = "说明", example = "xxx")
    String description;
}

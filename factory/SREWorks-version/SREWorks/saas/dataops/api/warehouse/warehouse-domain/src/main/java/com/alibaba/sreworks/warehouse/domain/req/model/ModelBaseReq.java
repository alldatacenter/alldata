package com.alibaba.sreworks.warehouse.domain.req.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 模型元信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="模型元信息")
public class ModelBaseReq {
    @ApiModelProperty(value = "模型名称(建议大写)", example = "APP", required = true)
    String name;

    @ApiModelProperty(value = "模型别名", example = "应用", required = true)
    String alias;

    @ApiModelProperty(hidden = true)
    Boolean buildIn;

    @ApiModelProperty(value = "数仓层次", example = "dwd", required = true)
    String layer;

    @ApiModelProperty(value = "分区规范(默认按天分区)", example = "d")
    String partitionFormat;

    @ApiModelProperty(value = "存储模式", example = "df")
    String dataMode;

    @ApiModelProperty(value = "统计周期", example = "1d/td")
    String statPeriod;

    @ApiModelProperty(value = "数据域ID", example = "1")
    Integer domainId;

    @ApiModelProperty(value = "生命周期(天)", example = "365")
    Integer lifecycle;

    @ApiModelProperty(value = "标签", example = "交付")
    String tag;

    @ApiModelProperty(value = "模型备注")
    String description;
}


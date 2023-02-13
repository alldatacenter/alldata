package com.alibaba.sreworks.warehouse.domain.req.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 实体元信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="实体元信息")
public class EntityBaseReq {
    @ApiModelProperty(value = "实体名称(建议大写)", example = "APP", required = true)
    String name;

    @ApiModelProperty(value = "实体别名", example = "应用", required = true)
    String alias;

    @ApiModelProperty(hidden = true)
    Boolean buildIn;

    @ApiModelProperty(hidden = true)
    String layer;

    @ApiModelProperty(value = "分区规范(默认按天分区)", example = "d")
    String partitionFormat;

    @ApiModelProperty(value = "生命周期(天)", example = "365")
    Integer lifecycle;

    @ApiModelProperty(hidden = true)
    String icon;

    @ApiModelProperty(value = "实体备注")
    String description;
}


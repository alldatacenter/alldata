package com.alibaba.sreworks.warehouse.domain.req.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 数据域信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="数据域信息")
public class DomainBaseReq {
    @ApiModelProperty(value = "域名", example = "系统域", required = true)
    String name;

    @ApiModelProperty(value = "域名简写(建议小写)", example = "system", required = true)
    String abbreviation;

    @ApiModelProperty(hidden = true)
    Boolean buildIn;

    @ApiModelProperty(value = "数仓主题")
    String subject;

    @ApiModelProperty(value = "数据域备注")
    String description;
}


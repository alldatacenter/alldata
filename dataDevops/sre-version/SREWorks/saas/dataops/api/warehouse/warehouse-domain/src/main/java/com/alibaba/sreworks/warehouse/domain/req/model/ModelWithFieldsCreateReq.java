package com.alibaba.sreworks.warehouse.domain.req.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 创建模型元信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="创建模型元信息(带列信息)")
public class ModelWithFieldsCreateReq {

    @ApiModelProperty(value = "模型元", required = true)
    ModelCreateReq metaReq;

    @ApiModelProperty(value = "模型列")
    ModelFieldCreateReq[] fieldsReq;

}


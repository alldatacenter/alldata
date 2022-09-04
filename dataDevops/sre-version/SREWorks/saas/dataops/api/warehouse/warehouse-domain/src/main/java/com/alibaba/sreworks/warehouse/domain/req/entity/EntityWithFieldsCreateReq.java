package com.alibaba.sreworks.warehouse.domain.req.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 创建实体元信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="创建实体元信息(带列信息)")
public class EntityWithFieldsCreateReq {

    @ApiModelProperty(value = "实体元", required = true)
    EntityCreateReq metaReq;

    @ApiModelProperty(value = "实体列")
    EntityFieldCreateReq[] fieldsReq;

}


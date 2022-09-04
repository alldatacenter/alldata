package com.alibaba.sreworks.warehouse.domain.req.model;

import com.alibaba.sreworks.warehouse.common.type.ColumnType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 模型列配置信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@Data
@ApiModel(value="模型列配置信息")
public class ModelFieldBaseReq {
    @ApiModelProperty(value = "字段名称", example = "xx", required = true)
    String field;

    @ApiModelProperty(value = "字段别名", example = "xxx", required = true)
    String alias;

    @ApiModelProperty(value = "存储列名", example = "yyy", required = true)
    String dim;

    @ApiModelProperty(value = "字段类型", example = "STRING", required = true)
    ColumnType type;

    @ApiModelProperty(hidden = true)
    Boolean buildIn;

    @ApiModelProperty(value = "是否允许为空", example = "yyy", required = true)
    Boolean nullable;

    @ApiModelProperty(value = "列说明")
    String description;
}

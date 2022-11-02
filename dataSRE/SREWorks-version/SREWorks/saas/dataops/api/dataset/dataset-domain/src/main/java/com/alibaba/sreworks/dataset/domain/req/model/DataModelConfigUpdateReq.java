package com.alibaba.sreworks.dataset.domain.req.model;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Setter;

/**
 * 模型配置信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="更新数据模型")
public class DataModelConfigUpdateReq extends DataModelConfigBaseReq {

    @Setter
    @ApiModelProperty(value = "模型ID", required = true)
    Integer id;

    public Integer getId() {
        Preconditions.checkArgument(id != null , "模型ID不允许为空");
        return id;
    }
}

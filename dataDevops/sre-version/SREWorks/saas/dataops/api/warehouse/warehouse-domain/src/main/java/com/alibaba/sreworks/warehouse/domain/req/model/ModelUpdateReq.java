package com.alibaba.sreworks.warehouse.domain.req.model;

import com.alibaba.sreworks.warehouse.common.constant.DwConstant;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 更新模型元信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="更新模型元信息")
public class ModelUpdateReq extends ModelBaseReq {
    @ApiModelProperty(value = "模型ID", required = true)
    Long id;

    public Long getId() {
        Preconditions.checkArgument(id != null, "模型id不允许为空");
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Integer getLifecycle() {
        if (lifecycle != null) {
            Preconditions.checkArgument((lifecycle >= DwConstant.MIN_LIFE_CYCLE) && (lifecycle <= DwConstant.MAX_LIFE_CYCLE),
                    String.format("生命周期参数非法,合理周期范围[%s, %s]", DwConstant.MIN_LIFE_CYCLE, DwConstant.MAX_LIFE_CYCLE));
        }
        return lifecycle;
    }
}


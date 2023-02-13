package com.alibaba.sreworks.health.domain.req.risk;


import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Setter;

/**
 * 更新风险类型请求
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/20 11:47
 */
@ApiModel(value = "更新风险类型")
public class RiskTypeUpdateReq extends RiskTypeBaseReq {
    @Setter
    @ApiModelProperty(value = "类型ID", example = "0")
    Integer id;

    public Integer getId() {
        Preconditions.checkArgument(id != null, "类型ID不允许为空");
        return this.id;
    }

}

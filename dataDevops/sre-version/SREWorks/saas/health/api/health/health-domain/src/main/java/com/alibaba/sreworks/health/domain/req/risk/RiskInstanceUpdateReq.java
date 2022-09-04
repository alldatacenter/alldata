package com.alibaba.sreworks.health.domain.req.risk;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Setter;

/**
 * 更新风险实例请求
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/05 16:27
 */
@ApiModel(value = "更新风险实例")
public class RiskInstanceUpdateReq extends RiskInstanceBaseReq {
    @Setter
    @ApiModelProperty(value = "实例ID", example = "0")
    Long id;

    public Long getId() {
        Preconditions.checkArgument(id != null, "实例ID不允许为空");
        return id;
    }

    @Override
    public Long getOccurTs() {
        if (occurTs != null && String.valueOf(occurTs).length() == 10) {
            occurTs = occurTs * 1000;
        }

        return occurTs;
    }
}

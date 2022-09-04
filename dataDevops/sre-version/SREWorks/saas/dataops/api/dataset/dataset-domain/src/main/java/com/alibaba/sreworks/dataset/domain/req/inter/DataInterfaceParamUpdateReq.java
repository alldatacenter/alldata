package com.alibaba.sreworks.dataset.domain.req.inter;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 数据接口参数信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="更新数据接口参数")
public class DataInterfaceParamUpdateReq extends DataInterfaceParamBaseReq {
    @ApiModelProperty(value = "参数ID", required = true)
    Long id;

    public Long getId() {
        Preconditions.checkArgument(id != null, "参数ID不允许为空");
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}

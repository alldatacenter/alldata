package com.alibaba.sreworks.dataset.domain.req.domain;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 模型配置信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="数据域更新信息")
public class DataDomainUpdateReq extends DataDomainBaseReq {
    @ApiModelProperty(value = "模型ID", required = true)
    Integer id;

    public Integer getId() {
        Preconditions.checkArgument(id != null, "domain id must not be empty");
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}

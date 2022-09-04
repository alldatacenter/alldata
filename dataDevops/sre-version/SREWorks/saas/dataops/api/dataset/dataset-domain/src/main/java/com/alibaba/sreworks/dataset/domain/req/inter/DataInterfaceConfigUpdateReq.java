package com.alibaba.sreworks.dataset.domain.req.inter;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

/**
 * 数据接口配置信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="更新数据接口配置")
public class DataInterfaceConfigUpdateReq extends DataInterfaceConfigBaseReq{
    @ApiModelProperty(value = "接口ID", required = true)
    Integer id;

    public Integer getId() {
        Preconditions.checkArgument(id != null, "接口ID不允许为空");
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String getRequestMethod() {
        if (StringUtils.isNotEmpty(requestMethod)) {
            return requestMethod.toUpperCase();
        }
        return requestMethod;
    }
}

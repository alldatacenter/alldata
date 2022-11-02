package com.alibaba.sreworks.dataset.domain.req.inter;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;

/**
 * 数据接口参数信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="创建数据接口参数")
public class InterfaceResponseParamCreateReq extends InterfaceResponseParamBaseReq {
    @Override
    public String getAlias() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(alias), "参数别名不允许为空");
        return alias;
    }

    @Override
    public String getName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "参数名称不允许为空");
        return name;
    }

    @Override
    public String getType() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(type), "参数类型不允许为空");
        return type.toUpperCase();
    }
}

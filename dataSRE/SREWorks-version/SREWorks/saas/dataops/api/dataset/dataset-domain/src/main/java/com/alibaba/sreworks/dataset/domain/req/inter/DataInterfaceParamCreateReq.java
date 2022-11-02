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
public class DataInterfaceParamCreateReq extends DataInterfaceParamBaseReq {
    @Override
    public String getLabel() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(label), "参数标识不允许为空");
        return label;
    }

    @Override
    public String getName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "参数名称不允许为空");
        return name;
    }

    @Override
    public Integer getInterfaceId() {
        Preconditions.checkArgument(interfaceId != null, "归属接口ID不允许为空");
        return interfaceId;
    }

    @Override
    public String getType() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(type), "参数类型不允许为空");
        return type.toUpperCase();
    }

    @Override
    public Boolean getRequired() {
        Preconditions.checkArgument(required != null, "参数是否必填不允许为空");
        return required;
    }
}

package com.alibaba.sreworks.dataset.domain.req.inter;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;

/**
 * 数据接口分组字段
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="数据接口分组字段")
public class InterfaceGroupFieldCreateReq extends InterfaceGroupFieldBaseReq {
    @Override
    public String getAlias() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(alias), "字段别名不允许为空");
        return alias;
    }

    @Override
    public String getDim() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(dim), "索引字段不允许为空");
        return dim;
    }

    @Override
    public String getType() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(type), "字段类型不允许为空");
        return type;
    }

    @Override
    public String getField() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(field), "字段名称不允许为空");
        return field;
    }
}

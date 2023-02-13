package com.alibaba.sreworks.health.domain.req.incident;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;

/**
 * 新增异常类型请求
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/20 11:47
 */
@ApiModel(value = "新增异常类型")
public class IncidentTypeCreateReq extends IncidentTypeBaseReq {

    @Override
    public String getLabel() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(label), "类型标识不允许为空");
        return label;
    }

    @Override
    public String getName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "类型名称不允许为空");
        return name;
    }
}

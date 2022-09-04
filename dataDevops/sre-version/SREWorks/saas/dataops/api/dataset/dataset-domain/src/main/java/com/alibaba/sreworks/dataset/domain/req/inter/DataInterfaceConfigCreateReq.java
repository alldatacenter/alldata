package com.alibaba.sreworks.dataset.domain.req.inter;

import com.alibaba.sreworks.dataset.common.constant.Constant;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;

/**
 * 数据接口配置信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="创建数据接口配置")
public class DataInterfaceConfigCreateReq extends DataInterfaceConfigBaseReq{
    @Override
    public String getLabel() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(label), "接口标识不允许为空");
        return label;
    }

    @Override
    public String getName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "接口名称不允许为空");
        return name;
    }

    @Override
    public Integer getModelId() {
        Preconditions.checkArgument(modelId != null, "归属模型ID不允许为空");
        return modelId;
    }

    @Override
    public Integer getTeamId() {
        return teamId != null ? teamId : Constant.SREWORKS_TEAM_ID;
    }

    @Override
    public String getRequestMethod() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(requestMethod), "请求方法不允许为空");
        return requestMethod.toUpperCase();
    }

    @Override
    public String getResponseFields() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(responseFields), "响应字段不允许为空");
        return responseFields;
    }

    @Override
    public Boolean getPaging() {
        return paging == null? false : paging;
    }
}

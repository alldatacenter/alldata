package com.alibaba.sreworks.dataset.domain.req.model;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;

/**
 * 模型配置信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="新增数据模型")
public class DataModelConfigCreateReq extends DataModelConfigBaseReq {
    @Override
    public String getName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name) , "模型名称不允许为空");
        return name;
    }

    @Override
    public String getLabel() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(label) , "模型标识不允许为空");
        return label;
    }

    @Override
    public Integer getDomainId() {
        Preconditions.checkArgument(domainId != null , "模型所属数据域ID不允许为空");
        return domainId;
    }

    @Override
    public Integer getTeamId() {
        Preconditions.checkArgument(teamId != null , "模型所属团队ID不允许为空");
        return teamId;
    }

    @Override
    public String getSourceType() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(sourceType) , "模型数据源不允许为空");
        return super.getSourceType();
    }

    @Override
    public String getSourceTable() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(sourceTable) , "模型源表不允许为空");
        return sourceTable;
    }
}

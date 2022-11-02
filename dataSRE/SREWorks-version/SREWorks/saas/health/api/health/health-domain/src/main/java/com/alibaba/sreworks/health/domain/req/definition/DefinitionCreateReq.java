package com.alibaba.sreworks.health.domain.req.definition;

import com.alibaba.sreworks.health.common.constant.Constant;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 事件定义模型基类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/19 16:30
 */
@Data
@ApiModel(value = "新增事件定义")
public class DefinitionCreateReq extends DefinitionBaseReq {
    @Override
    public String getName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "定义名称不允许为空");
        return name;
    }

    @Override
    public String getCategory() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(category) && Constant.DEFINITION_CATEGORIES.contains(category.toLowerCase()),
                "定义分类非法, 合法取值:" + Constant.DEFINITION_CATEGORIES.toString());
        return category.toLowerCase();
    }

    @Override
    public String getAppId() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(appId) , "归属应用不允许为空");
        return  appId;
    }

    @Override
    public String getAppName() {
        return  StringUtils.isEmpty(appName) ? getAppId() : appName;
    }


    @Override
    public DefinitionExConfigReq getExConfig() {
        return exConfig;
    }
}

package com.alibaba.sreworks.health.domain.req.definition;

import com.alibaba.sreworks.health.common.constant.Constant;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * 事件定义模型基类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/19 16:30
 */
@Data
@ApiModel(value = "新增事件定义")
public class DefinitionUpdateReq extends DefinitionBaseReq {
    @Setter
    @ApiModelProperty(value = "定义ID", required = true, example = "1")
    Integer id;

    public Integer getId() {
        Preconditions.checkArgument(id != null, "定义ID不允许为空");
        return id;
    }

    @Override
    public String getCategory() {
        if (StringUtils.isNotEmpty(category)) {
            Preconditions.checkArgument(Constant.DEFINITION_CATEGORIES.contains(category.toLowerCase()),
                    "定义分类合法取值:" + Constant.DEFINITION_CATEGORIES.toString());

            return category.toLowerCase();
        }

        return category;
    }
}

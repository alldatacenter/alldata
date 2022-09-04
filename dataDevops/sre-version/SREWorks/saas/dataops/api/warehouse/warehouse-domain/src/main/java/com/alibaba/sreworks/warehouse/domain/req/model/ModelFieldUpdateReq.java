package com.alibaba.sreworks.warehouse.domain.req.model;

import com.alibaba.sreworks.warehouse.common.constant.DwConstant;
import com.alibaba.sreworks.warehouse.common.utils.Regex;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

/**
 * 更新模型列
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="更新模型列")
public class ModelFieldUpdateReq extends ModelFieldBaseReq {
    @ApiModelProperty(value = "列ID", required = true)
    Long id;

    public Long getId() {
        Preconditions.checkArgument(id != null, "列id不允许为空");
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getField() {
        if (StringUtils.isNotEmpty(field)) {
            Preconditions.checkArgument(Regex.checkDocumentByPattern(field, DwConstant.ENTITY_MODEL_FIELD_PATTERN), "模型列名不符合规范:" + DwConstant.ENTITY_MODEL_FIELD_REGEX);
        }
        return field;
    }

    @Override
    public String getDim() {
        if (StringUtils.isNotEmpty(dim))  {
            Preconditions.checkArgument(Regex.checkDocumentByPattern(dim, DwConstant.ENTITY_MODEL_DIM_FIELD_PATTERN), "存储列名不符合规范:" + DwConstant.ENTITY_MODEL_DIM_FIELD_REGEX);
        }
        return dim;
    }

    @Override
    public Boolean getBuildIn() {
        return false;
    }
}

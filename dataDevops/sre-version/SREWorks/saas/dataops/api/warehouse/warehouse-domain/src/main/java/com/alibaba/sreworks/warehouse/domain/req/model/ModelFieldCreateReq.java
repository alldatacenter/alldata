package com.alibaba.sreworks.warehouse.domain.req.model;

import com.alibaba.sreworks.warehouse.common.constant.DwConstant;
import com.alibaba.sreworks.warehouse.common.type.ColumnType;
import com.alibaba.sreworks.warehouse.common.utils.Regex;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

/**
 * 新增模型列
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="新增模型列")
public class ModelFieldCreateReq extends ModelFieldBaseReq {
    @Override
    public String getField() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(field), "模型列名不允许为空");
        Preconditions.checkArgument(Regex.checkDocumentByPattern(field, DwConstant.ENTITY_MODEL_FIELD_PATTERN), "模型列名不符合规范:" + DwConstant.ENTITY_MODEL_FIELD_REGEX);
        return field;
    }

    @Override
    public String getAlias() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(alias), "模型列别名不允许为空");
        return alias;
    }

    @Override
    public String getDim() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(dim), "存储列名不允许为空");
        Preconditions.checkArgument(Regex.checkDocumentByPattern(dim, DwConstant.ENTITY_MODEL_DIM_FIELD_PATTERN), "存储列名不符合规范:" + DwConstant.ENTITY_MODEL_DIM_FIELD_REGEX);
        return dim;
    }

    @Override
    public Boolean getBuildIn() {
        if (ObjectUtils.isEmpty(buildIn)) {
            return false;
        }
        return buildIn;
    }

    @Override
    public ColumnType getType() {
        Preconditions.checkArgument(type != null, "模型列类型不允许为空");
        return type;
    }

    @Override
    public Boolean getNullable() {
        Preconditions.checkArgument(nullable != null, "模型列是否为空必填");
        return nullable;
    }
}

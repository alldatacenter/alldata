package com.alibaba.sreworks.warehouse.domain.req.entity;

import com.alibaba.sreworks.warehouse.common.constant.DwConstant;
import com.alibaba.sreworks.warehouse.common.type.ColumnType;
import com.alibaba.sreworks.warehouse.common.utils.Regex;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

/**
 * 新增实体列
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */
@ApiModel(value="新增实体列")
public class EntityFieldCreateReq extends EntityFieldBaseReq {
    @Override
    public String getField() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(field), "实体列名不允许为空");
        Preconditions.checkArgument(Regex.checkDocumentByPattern(field, DwConstant.ENTITY_MODEL_FIELD_PATTERN), "实体列名不符合规范:" + DwConstant.ENTITY_MODEL_FIELD_REGEX);
        return field;
    }

    @Override
    public String getAlias() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(alias), "实体列别名不允许为空");
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
        return type;
    }

    @Override
    public Boolean getNullable() {
        return nullable;
    }
}

package com.alibaba.sreworks.warehouse.domain.req.entity;

import com.alibaba.sreworks.warehouse.common.constant.DwConstant;
import com.alibaba.sreworks.warehouse.common.utils.Regex;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

/**
 * 创建实体元信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="创建实体元信息")
public class EntityCreateReq extends EntityBaseReq {
    @Override
    public String getName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "实体名称不允许为空");
        Preconditions.checkArgument(Regex.checkDocumentByPattern(name, DwConstant.ENTITY_MODEL_NAME_PATTERN), "实体名称不符合规范:" + DwConstant.ENTITY_MODEL_NAME_REGEX);
        return name;
    }

    @Override
    public String getAlias() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(alias), "实体别名不允许为空");
        return alias;
    }

    @Override
    public Boolean getBuildIn() {
        if (ObjectUtils.isEmpty(buildIn)) {
            return false;
        }
        return buildIn;
    }

    @Override
    public String getLayer() {
        return DwConstant.DW_ODS_LAYER;
    }

    @Override
    public String getPartitionFormat() {
        partitionFormat = partitionFormat == null ? DwConstant.PARTITION_BY_DAY : partitionFormat;
        Preconditions.checkArgument(DwConstant.PARTITION_FORMATS.contains(partitionFormat), "分区规范非法, 合法取值" + DwConstant.PARTITION_FORMATS);
        return partitionFormat;
    }

    @Override
    public Integer getLifecycle() {
        lifecycle = lifecycle == null ? DwConstant.DEFAULT_LIFE_CYCLE : lifecycle;
        Preconditions.checkArgument((lifecycle >= DwConstant.MIN_LIFE_CYCLE) && (lifecycle <= DwConstant.MAX_LIFE_CYCLE),
                String.format("生命周期参数非法,合理周期范围[%s, %s]", DwConstant.MIN_LIFE_CYCLE, DwConstant.MAX_LIFE_CYCLE));
        return lifecycle;
    }
}


package com.alibaba.sreworks.warehouse.domain.req.model;

import com.alibaba.sreworks.warehouse.common.constant.DwConstant;
import com.alibaba.sreworks.warehouse.common.utils.Regex;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

/**
 * 创建模型元信息
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 17:04
 */

@ApiModel(value="创建模型元信息")
public class ModelCreateReq extends ModelBaseReq {
    @Override
    public String getName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "模型名称不允许为空");
        Preconditions.checkArgument(Regex.checkDocumentByPattern(name, DwConstant.ENTITY_MODEL_NAME_PATTERN), "模型名称不符合规范:" + DwConstant.ENTITY_MODEL_NAME_REGEX);
        return name;
    }

    @Override
    public String getAlias() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(alias), "模型别名不允许为空");
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
        Preconditions.checkArgument(StringUtils.isNotEmpty(layer), "数仓分层不允许为空");
        layer = layer.toLowerCase();
        Preconditions.checkArgument(DwConstant.DW_LAYERS.contains(layer), "数仓分层非法, 合法取值" + DwConstant.DW_LAYERS);
        return layer;
    }

    @Override
    public String getPartitionFormat() {
        partitionFormat = partitionFormat == null ? DwConstant.PARTITION_BY_DAY : partitionFormat;
        Preconditions.checkArgument(DwConstant.PARTITION_FORMATS.contains(partitionFormat), "分区规范非法, 合法取值" + DwConstant.PARTITION_FORMATS);
        return partitionFormat;
    }

    @Override
    public String getDataMode() {
        if (DwConstant.DW_DWD_LAYER.equals(layer)) {
            dataMode = dataMode == null ? DwConstant.PARTITION_DATA_MODE_DF : dataMode;
            Preconditions.checkArgument(DwConstant.PARTITION_DATA_MODE.contains(dataMode), "存储模式非法, 合法取值" + DwConstant.PARTITION_DATA_MODE);
        } else {
            dataMode = null;
        }
        return dataMode;
    }

    @Override
    public String getStatPeriod() {
        if (DwConstant.DW_DWS_LAYER.equals(layer)) {
            Preconditions.checkArgument(StringUtils.isNotEmpty(statPeriod) && DwConstant.STAT_PERIODS.contains(statPeriod), "统计周期非法, 合法取值" + DwConstant.STAT_PERIODS);
        } else {
            statPeriod = null;
        }

        return statPeriod;
    }

    @Override
    public Integer getDomainId() {
        Preconditions.checkArgument((DwConstant.DW_ADS_LAYER.equals(layer) && domainId == null) || (DwConstant.DW_CDM_LAYERS.contains(layer) && domainId != null), "CDM层数据域不允许为空");
        return domainId;
    }

    @Override
    public Integer getLifecycle() {
        lifecycle = lifecycle == null ? DwConstant.DEFAULT_LIFE_CYCLE : lifecycle;
        Preconditions.checkArgument((lifecycle >= DwConstant.MIN_LIFE_CYCLE) && (lifecycle <= DwConstant.MAX_LIFE_CYCLE),
                String.format("生命周期参数非法,合理周期范围[%s, %s]", DwConstant.MIN_LIFE_CYCLE, DwConstant.MAX_LIFE_CYCLE));
        return lifecycle;
    }

    @Override
    public String getTag() {
        if (getLayer().equals(DwConstant.DW_ADS_LAYER)) {
            Preconditions.checkArgument(StringUtils.isNotEmpty(tag), "应用层需要提供模型标签");
            Preconditions.checkArgument(DwConstant.ADS_MODEL_TAGS.contains(tag), "应用层目前支持标签" + DwConstant.ADS_MODEL_TAGS);
        }
        return tag;
    }
}


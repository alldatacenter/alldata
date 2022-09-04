package com.alibaba.sreworks.dataset.domain.bo;

import com.alibaba.sreworks.dataset.common.constant.ValidConstant;
import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * 数据接口排序字段
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/21 15:06
 */
@Data
@NoArgsConstructor
public class DataInterfaceSortField {
    String fieldName;

    String order;

    String mode;

    String format;

    public String getFieldName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(fieldName) , "排序字段名称不允许为空");
        return fieldName;
    }

    public String getOrder() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(order) , "排序类型不允许为空");
        Preconditions.checkArgument(ValidConstant.ORDER_TYPE_LIST.contains(order), String.format("不支持排序类型%s, 仅支持%s", order, ValidConstant.ORDER_TYPE_LIST));
        return order;
    }

    public String getMode() {
        if (StringUtils.isNotEmpty(mode)) {
            Preconditions.checkArgument(ValidConstant.ES_SORT_MODE_LIST.contains(mode), String.format("不支持排序模式%s, 仅支持%s", mode, ValidConstant.ES_SORT_MODE_LIST));
        }
        return mode;
    }

    public String getFormat() {
        if (StringUtils.isNotEmpty(format)) {
            Preconditions.checkArgument(ValidConstant.ES_SORT_FORMAT_LIST.contains(format), String.format("不支持格式化方式%s, 仅支持%s", format, ValidConstant.ES_SORT_FORMAT_LIST));
        }
        return format;
    }
}

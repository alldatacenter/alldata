package com.alibaba.sreworks.dataset.domain.bo;

import com.alibaba.sreworks.dataset.common.constant.ValidConstant;
import com.alibaba.sreworks.dataset.common.exception.ModelConfigException;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * 模型数值字段对象
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/08/12 19:08
 */

@Data
@NoArgsConstructor
public class DataModelValueField {

    String operator;

    String dim;

    String type;

    String field;

    String description;

    /**
     * ES数据源 模型数值字段对象合法性校验
     * @throws Exception
     */
    public void boEsCheck() throws Exception {
        boCheck();
        if (StringUtils.isNotEmpty(operator) && !ValidConstant.ES_FIELD_AGG_TYPE_LIST.contains(operator)) {
            throw new ModelConfigException(String.format("不支持计算类型%s, 目前仅支持%s", operator, ValidConstant.ES_FIELD_AGG_TYPE_LIST));
        }
    }

    private void boCheck() throws Exception {
        if (StringUtils.isEmpty(dim) || StringUtils.isEmpty(type) || StringUtils.isEmpty(field)) {
            throw new ModelConfigException("数据模型数值字段配置错误, 字段标识/字段内容/字段类型均不允许为空");
        }
    }
}

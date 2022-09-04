package com.alibaba.sreworks.dataset.domain.bo;

import com.alibaba.sreworks.dataset.common.constant.ValidConstant;
import com.alibaba.sreworks.dataset.common.exception.ModelConfigException;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * 模型分组字段对象
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/08/12 19:08
 */

@Data
@NoArgsConstructor
public class DataModelGroupField {

    String operator;

    String dim;

    String type;

    String field;

    String description;

    /**
     * ES数据源 模型分组字段对象合法性校验 分组仅支持 date_histogram terms两种
     * @throws Exception
     */
    public void boEsCheck() throws Exception {
        boCheck();
        if (StringUtils.isEmpty(operator)) {
            throw new ModelConfigException("数据模型分组字段配置错误");
        }

        if (!ValidConstant.ES_BUCKET_AGG_TYPE_LIST.contains(operator)) {
            throw new ModelConfigException(String.format("不支持聚合类型%s", operator));
        }
    }

    private void boCheck() throws Exception {
        if (StringUtils.isEmpty(dim) || StringUtils.isEmpty(type) || StringUtils.isEmpty(field)) {
            throw new ModelConfigException("数据模型分组字段配置错误， 字段标识/字段内容/字段类型均不允许为空");
        }
    }
}

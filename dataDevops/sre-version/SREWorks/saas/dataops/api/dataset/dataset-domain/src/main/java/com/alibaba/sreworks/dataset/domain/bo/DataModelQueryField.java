package com.alibaba.sreworks.dataset.domain.bo;

import com.alibaba.sreworks.dataset.common.exception.ModelConfigException;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * 模型查询字段对象
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/08/12 19:08
 */

@Data
@NoArgsConstructor
public class DataModelQueryField {
    String type;

    String field;

    String description;

    private void boCheck() throws Exception {
        if (StringUtils.isEmpty(type) || StringUtils.isEmpty(field)) {
            throw new ModelConfigException("数据模型查询字段配置错误， 字段标识/字段类型均不允许为空");
        }
    }
}

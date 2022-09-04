package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 应用市场查询条件类
 *
 * @author qianmo.zm@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AppMarketQueryCondition extends BaseCondition {

    /**
     * 标签
     */
    private String tag;

    /**
     * 应用选项过滤 Key
     */
    private String optionKey;

    /**
     * 应用选项过滤 Value
     */
    private String optionValue;
}

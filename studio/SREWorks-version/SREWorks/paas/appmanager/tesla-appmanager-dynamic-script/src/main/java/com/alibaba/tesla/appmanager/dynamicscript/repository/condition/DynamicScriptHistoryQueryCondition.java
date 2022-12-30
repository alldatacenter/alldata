package com.alibaba.tesla.appmanager.dynamicscript.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 动态脚本历史表查询 Condition
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicScriptHistoryQueryCondition extends BaseCondition {

    /**
     * 标识名称
     */
    private String name;

    /**
     * 版本号
     */
    private Integer revision;
}

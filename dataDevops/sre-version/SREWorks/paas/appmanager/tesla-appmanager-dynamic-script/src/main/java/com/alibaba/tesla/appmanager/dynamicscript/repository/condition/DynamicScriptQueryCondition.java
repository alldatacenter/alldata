package com.alibaba.tesla.appmanager.dynamicscript.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 动态脚本查询 Condition
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicScriptQueryCondition extends BaseCondition {

    /**
     * 脚本类型
     */
    private String kind;

    /**
     * 标识名称
     */
    private String name;
}

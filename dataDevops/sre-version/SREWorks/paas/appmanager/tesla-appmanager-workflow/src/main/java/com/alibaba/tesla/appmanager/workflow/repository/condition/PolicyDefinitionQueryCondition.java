package com.alibaba.tesla.appmanager.workflow.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * PolicyDefinition 查询条件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDefinitionQueryCondition extends BaseCondition {

    /**
     * Policy 类型
     */
    private String policyType;
}

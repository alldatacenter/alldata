package com.alibaba.tesla.appmanager.workflow.service.impl;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.workflow.repository.condition.PolicyDefinitionQueryCondition;
import com.alibaba.tesla.appmanager.workflow.repository.domain.PolicyDefinitionDO;
import com.alibaba.tesla.appmanager.workflow.service.PolicyDefinitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Policy 定义服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class PolicyDefinitionServiceImpl implements PolicyDefinitionService {

    /**
     * 根据指定条件查询对应的 PolicyDefinition 列表
     *
     * @param condition 条件
     * @param operator  操作人
     * @return Page of PolicyDefinition
     */
    @Override
    public Pagination<PolicyDefinitionDO> list(PolicyDefinitionQueryCondition condition, String operator) {
        return null;
    }

    /**
     * 根据指定条件查询对应的 PolicyDefinition (期望只返回一个)
     *
     * @param condition 条件
     * @param operator  操作人
     * @return Page of PolicyDefinition
     */
    @Override
    public PolicyDefinitionDO get(PolicyDefinitionQueryCondition condition, String operator) {
        return null;
    }

    /**
     * 向系统中新增或更新一个 PolicyDefinition
     *
     * @param request  记录的值
     * @param operator 操作人
     */
    @Override
    public void apply(PolicyDefinitionDO request, String operator) {

    }

    /**
     * 删除指定条件的 PolicyDefinition (必须传入 policyType 参数明确删除对象)
     *
     * @param condition 条件
     * @param operator  操作人
     * @return 删除的数量 (0 or 1)
     */
    @Override
    public int delete(PolicyDefinitionQueryCondition condition, String operator) {
        return 0;
    }
}

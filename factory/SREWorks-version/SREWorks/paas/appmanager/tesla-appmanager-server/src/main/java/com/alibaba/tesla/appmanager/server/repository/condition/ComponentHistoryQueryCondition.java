package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 组件历史表查询 Condition
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentHistoryQueryCondition extends BaseCondition {

    /**
     * 组件类型，全局唯一
     */
    private String componentType;

    /**
     * 适配类型，可选 core, groovy
     */
    private String componentAdapterType;

    /**
     * 版本号
     */
    private Integer revision;
}

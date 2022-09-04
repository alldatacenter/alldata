package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 单元查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UnitQueryCondition extends BaseCondition {

    /**
     * 单元唯一标识
     */
    private String unitId;

    /**
     * 单元名称
     */
    private String unitName;

    /**
     * 分类
     */
    private String category;
}

package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * App 选项查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AppOptionQueryCondition extends BaseCondition {

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * Key
     */
    private String key;

    /**
     * Value
     */
    private String value;
}

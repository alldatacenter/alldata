package com.alibaba.tesla.appmanager.domain.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用于实例侧的 Condition 描述 (for ComponentInstance / TraitInstance)
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceCondition implements Serializable {

    /**
     * 类型
     */
    private String type;

    /**
     * 状态
     */
    private String status;

    /**
     * 机器可读原因
     */
    private String reason;

    /**
     * 人类可读原因
     */
    private String message;
}

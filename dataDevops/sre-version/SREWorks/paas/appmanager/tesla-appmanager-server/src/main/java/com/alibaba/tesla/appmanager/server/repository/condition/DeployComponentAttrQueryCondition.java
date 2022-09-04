package com.alibaba.tesla.appmanager.server.repository.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Component 部署单属性查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployComponentAttrQueryCondition implements Serializable {

    private static final long serialVersionUID = -7178725103772290552L;

    /**
     * 部署工单 ID
     */
    private Long deployComponentId;

    /**
     * 部署工单 ID 列表
     */
    private List<Long> deployComponentIdList;

    /**
     * 属性类型 (optional)
     */
    private String attrType;
}

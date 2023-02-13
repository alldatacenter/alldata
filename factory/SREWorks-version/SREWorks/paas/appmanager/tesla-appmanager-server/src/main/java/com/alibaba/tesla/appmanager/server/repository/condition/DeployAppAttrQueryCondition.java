package com.alibaba.tesla.appmanager.server.repository.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * App 部署单属性查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployAppAttrQueryCondition implements Serializable {

    private static final long serialVersionUID = -8970502503339188493L;

    /**
     * 部署工单 ID
     */
    private Long deployAppId;

    /**
     * 部署工单 ID 列表
     */
    private List<Long> deployAppIdList;

    /**
     * 属性类型 (optional)
     */
    private String attrType;
}

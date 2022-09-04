package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import com.alibaba.tesla.appmanager.common.enums.DeployComponentStateEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Component 部署单查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DeployComponentQueryCondition extends BaseCondition {

    private String clusterId;

    private String namespaceId;

    private String stageId;

    private String appId;

    private Long deployAppId;

    private String identifier;

    private String identifierStartsWith;

    private DeployComponentStateEnum deployStatus;

    private List<DeployComponentStateEnum> deployStatusList;

    private Long deployProcessId;

    private String orderBy;
}

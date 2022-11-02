package com.alibaba.tesla.appmanager.server.repository.condition;

import com.alibaba.tesla.appmanager.common.BaseCondition;
import com.alibaba.tesla.appmanager.common.enums.DeployAppStateEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * App 部署单查询条件类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DeployAppQueryCondition extends BaseCondition {

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 集群 ID
     */
    private String clusterId;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * 部署流程 ID
     */
    private Long deployProcessId;

    /**
     * 应用包 ID
     */
    private Long appPackageId;

    /**
     * 包版本
     */
    private String packageVersion;

    /**
     * stageId 的白名单
     */
    private List<String> stageIdWhiteList;

    /**
     * stageId 的黑名单
     */
    private List<String> stageIdBlackList;

    /**
     * 部署状态
     */
    private DeployAppStateEnum deployStatus;

    /**
     * 应用 Option Key
     */
    private String optionKey;

    /**
     * 应用 Option Value
     */
    private String optionValue;
}

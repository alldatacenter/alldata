package com.alibaba.tesla.appmanager.domain.req.deploy;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 拉取 Deploy App 部署工单的清单过滤请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DeployAppListReq extends BaseRequest {

    /**
     * 部署单 ID 列表
     */
    private List<Long> deployAppIdList = new ArrayList<>();

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
     * 部署状态
     */
    private String deployStatus;

    /**
     * 包版本
     */
    private String packageVersion;

    /**
     * stageId的白名单
     */
    private List<String> stageIdWhiteList;

    /**
     * stageId的黑名单
     */
    private List<String> stageIdBlackList;

    /**
     * 应用 Option Key
     */
    private String optionKey;

    /**
     * 应用 Option Value
     */
    private String optionValue;
}

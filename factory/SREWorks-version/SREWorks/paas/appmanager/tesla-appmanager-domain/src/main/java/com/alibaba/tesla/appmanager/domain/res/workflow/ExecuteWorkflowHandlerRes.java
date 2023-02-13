package com.alibaba.tesla.appmanager.domain.res.workflow;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 执行 Workflow Handler 返回结果
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteWorkflowHandlerRes implements Serializable {

    /**
     * 修改后的上下文 context 信息
     */
    private JSONObject context;

    /**
     * 修改后的 configuration 部署配置信息
     */
    private DeployAppSchema configuration;

    /**
     * 如果当前 workflow 触发了部署，那么此处返回部署单 ID
     */
    private Long deployAppId;

    /**
     * 部署单归属 Unit ID
     */
    private String deployAppUnitId;

    /**
     * 部署单归属 Namespace ID
     */
    private String deployAppNamespaceId;

    /**
     * 部署单归属 Stage ID
     */
    private String deployAppStageId;

    /**
     * 是否暂停
     */
    private boolean suspend;
}

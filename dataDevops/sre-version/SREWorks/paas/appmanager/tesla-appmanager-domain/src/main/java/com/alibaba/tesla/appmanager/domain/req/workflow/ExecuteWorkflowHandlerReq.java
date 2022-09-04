package com.alibaba.tesla.appmanager.domain.req.workflow;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 执行 Workflow Handler 请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteWorkflowHandlerReq implements Serializable {

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * Workflow Instance ID
     */
    private Long instanceId;

    /**
     * Workflow Task ID
     */
    private Long taskId;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 任务运行阶段
     */
    private String taskStage;

    /**
     * 任务属性
     */
    private JSONObject taskProperties;

    /**
     * 上下文
     */
    private JSONObject context;

    /**
     * Workflow 配置信息
     */
    private DeployAppSchema configuration;

    /**
     * 创建者
     */
    private String creator;
}

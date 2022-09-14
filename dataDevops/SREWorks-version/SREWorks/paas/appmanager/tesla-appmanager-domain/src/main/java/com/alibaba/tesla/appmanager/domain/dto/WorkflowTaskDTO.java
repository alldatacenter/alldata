package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Workflow 任务 DTO
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowTaskDTO {

    /**
     * ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * 批次 ID
     */
    private Long batchId;

    /**
     * Workflow 实例 ID
     */
    private Long workflowInstanceId;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 开始时间
     */
    private Date gmtStart;

    /**
     * 结束时间
     */
    private Date gmtEnd;

    /**
     * Workflow 任务类型
     */
    private String taskType;

    /**
     * Workflow 任务节点运行阶段 (pre-render, post-render, post-deploy)
     */
    private String taskStage;

    /**
     * Workflow 任务节点状态 (PENDING, RUNNING, WAITING[完成UserFunction后等待完成], SUSPEND, SUCCESS, FAILURE, EXCEPTION, TERMINATED)
     */
    private String taskStatus;

    /**
     * Workflow 任务当前所在执行节点 Hostname
     */
    private String clientHostname;

    /**
     * 部署单 ID
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
     * 乐观锁版本
     */
    private Integer lockVersion;

    /**
     * Workflow 任务节点属性 (JSONObject 字符串)
     */
    private JSONObject taskProperties;

    /**
     * Workflow 任务节点执行出错信息 (仅 task_status==EXCEPTIOIN 下存在)
     */
    private String taskErrorMessage;
}
package com.alibaba.tesla.appmanager.domain.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Workflow 实例 DTO
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowInstanceDTO {
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
     * 工作流状态 (PENDING, RUNNING, SUSPEND, SUCCESS, FAILURE, EXCEPTION, TERMINATED)
     */
    private String workflowStatus;

    /**
     * 工作流执行出错信息 (仅 workflow_status==EXCEPTION 下存在)
     */
    private String workflowErrorMessage;

    /**
     * Workflow Configuration
     */
    private String workflowConfiguration;

    /**
     * Workflow Configuration SHA256
     */
    private String workflowSha256;

    /**
     * Workflow 启动选项 (JSON)
     */
    private JSONObject workflowOptions;

    /**
     * 创建人
     */
    private String workflowCreator;

    /**
     * 乐观锁版本
     */
    private Integer lockVersion;
}
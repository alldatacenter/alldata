package com.alibaba.tesla.appmanager.domain.req.workflow;

import com.alibaba.tesla.appmanager.domain.option.WorkflowInstanceOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowLaunchReq implements Serializable {

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * Workflow 配置内容
     */
    private String configuration;

    /**
     * 应用 ID
     */
    private WorkflowInstanceOption options;
}

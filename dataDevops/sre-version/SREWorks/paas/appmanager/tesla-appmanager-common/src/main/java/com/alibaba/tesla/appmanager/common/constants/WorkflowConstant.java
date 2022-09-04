package com.alibaba.tesla.appmanager.common.constants;

import com.alibaba.tesla.appmanager.common.enums.WorkflowStageEnum;

/**
 * Workflow 相关常量
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class WorkflowConstant {

    /**
     * 默认 Workflow 类型
     */
    public static final String DEFAULT_WORKFLOW_TYPE = "deploy";

    /**
     * 默认 Workflow 运行阶段
     */
    public static final WorkflowStageEnum DEFAULT_WORKFLOW_STAGE = WorkflowStageEnum.PRE_RENDER;
}

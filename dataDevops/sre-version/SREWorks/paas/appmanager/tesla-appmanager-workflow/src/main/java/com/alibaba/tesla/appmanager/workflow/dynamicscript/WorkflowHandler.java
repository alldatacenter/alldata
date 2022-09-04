package com.alibaba.tesla.appmanager.workflow.dynamicscript;

import com.alibaba.tesla.appmanager.domain.req.workflow.ExecuteWorkflowHandlerReq;
import com.alibaba.tesla.appmanager.domain.res.workflow.ExecuteWorkflowHandlerRes;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandler;

/**
 * 工作流 Groovy Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface WorkflowHandler extends GroovyHandler {

    ExecuteWorkflowHandlerRes execute(ExecuteWorkflowHandlerReq request) throws InterruptedException;
}

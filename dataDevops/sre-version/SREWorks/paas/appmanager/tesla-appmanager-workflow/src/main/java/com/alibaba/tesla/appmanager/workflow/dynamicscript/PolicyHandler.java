package com.alibaba.tesla.appmanager.workflow.dynamicscript;

import com.alibaba.tesla.appmanager.domain.req.workflow.ExecutePolicyHandlerReq;
import com.alibaba.tesla.appmanager.domain.res.workflow.ExecutePolicyHandlerRes;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandler;

/**
 * Policy Groovy Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface PolicyHandler extends GroovyHandler {

    ExecutePolicyHandlerRes execute(ExecutePolicyHandlerReq request) throws InterruptedException;
}

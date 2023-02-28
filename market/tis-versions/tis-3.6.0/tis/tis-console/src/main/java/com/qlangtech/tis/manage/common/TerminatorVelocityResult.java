///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//package com.qlangtech.tis.manage.common;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.struts2.ServletActionContext;
//import org.apache.struts2.result.VelocityResult;
//import com.opensymphony.xwork2.ActionInvocation;
//import com.qlangtech.tis.runtime.module.action.BasicModule;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2016年5月11日
// */
//public class TerminatorVelocityResult extends VelocityResult {
//
//    private static final long serialVersionUID = 1L;
//
//    @Override
//    public void execute(ActionInvocation invocation) throws Exception {
//        final String lastFinalLocation = conditionalParse(this.getLocation(), invocation);
//        if (!BasicModule.isScreenApply()) {
//            doExecute(lastFinalLocation, invocation);
//            return;
//        }
//        setPlaceholder(invocation, lastFinalLocation);
//        expressLayout(invocation);
//    }
//
//    public void setPlaceholder(ActionInvocation invocation, final String lastFinalLocation) {
//        invocation.getInvocationContext().put("screen_placeholder", lastFinalLocation);
//    }
//
//    public void expressLayout(ActionInvocation invocation) throws Exception {
//        HttpServletRequest request = ServletActionContext.getRequest();
//        String layout = request.getParameter("vmlayout");
//        String overrideLayoutTemplate = (String) request.getAttribute(BasicModule.Layout_template);
//        if (// && invocation.getAction() instanceof IModalDialog
//        StringUtils.isEmpty(layout)) {
//            overrideLayoutTemplate = "modal";
//        }
//        overrideLayoutTemplate = StringUtils.defaultString(layout, overrideLayoutTemplate);
//        if (overrideLayoutTemplate == null) {
//            doExecute("/runtime/templates/layout/blank.vm", invocation);
//        } else {
//            doExecute("/runtime/templates/layout/" + overrideLayoutTemplate + ".vm", invocation);
//        }
//        // 是否要执行bigpipe
//        BasicModule basicAction = (BasicModule) invocation.getAction();
//        HttpServletResponse response = ServletActionContext.getResponse();
//        basicAction.doBigPipe(response.getOutputStream());
//    }
//}

/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.common.valve;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.MethodFilterInterceptor;
import com.qlangtech.tis.manage.spring.aop.AuthorityCheckAdvice;
import org.apache.struts2.ServletActionContext;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ServerGroupExistInterceptor extends MethodFilterInterceptor {

  private static final long serialVersionUID = 1L;

  @Override
  protected boolean applyInterceptor(ActionInvocation invocation) {
    return AuthorityCheckAdvice.inNotForwardProcess();
  }

  @Override
  protected String doIntercept(ActionInvocation invocation) throws Exception {
    // BasicScreen
    HttpServletRequest request = ServletActionContext.getRequest();
    Object process = request.getAttribute(ServerGroupExistInterceptor.class.getName());
    if (process != null) {
      return invocation.invoke();
    }
    try {
      return invocation.invoke();
    } finally {
      request.setAttribute(ServerGroupExistInterceptor.class.getName(), new Object());
    }
  }
}

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

import com.alibaba.citrus.turbine.Context;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.interceptor.MethodFilterInterceptor;
import com.qlangtech.tis.manage.biz.dal.dao.IOperationLogDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.OperationLog;
import com.qlangtech.tis.manage.common.*;
import com.qlangtech.tis.manage.spring.aop.AuthorityCheckAdvice;
import com.qlangtech.tis.manage.spring.aop.Func;
import com.qlangtech.tis.pubhook.common.Nullable;
import com.qlangtech.tis.runtime.module.action.BasicModule;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-07-13 13:21
 */
public class OperationLogInterceptor extends MethodFilterInterceptor {

  private IOperationLogDAO operationLogDAO;

  private RunContext daoContext;

  @Override
  protected boolean applyInterceptor(ActionInvocation invocation) {

    return AuthorityCheckAdvice.inNotForwardProcess();
  }

  @Override
  protected String doIntercept(ActionInvocation invocation) throws Exception {

    BasicModule action = (BasicModule) invocation.getAction();
    final Method method = action.getExecuteMethod();
    final String result = invocation.invoke();

    Func func = method.getAnnotation(Func.class);
    if (func != null && !func.sideEffect()) {
      return result;
    }
    AppDomainInfo appDomain = CheckAppDomainExistValve.getAppDomain(daoContext);
    OperationLog log = new OperationLog();
    log.setOpType(BasicModule.parseMehtodName());
    if (StringUtils.startsWith(log.getOpType(), "doGet")) {
      return result;
    }
    log.setCreateTime(new Date());
    if (!(appDomain instanceof Nullable)) {
      log.setAppName(appDomain.getAppName());
    }
    ActionProxy proxy = invocation.getProxy();
    // log.setOpType(proxy.getMethod());
    HttpServletRequest request = ServletActionContext.getRequest();

    if (StringUtils.indexOf(request.getClass().getSimpleName(), "Mock") > -1) {
      // 当前是测试流程的话就不需要记录日志了
      return result;
    }
    IUser user = UserUtils.getUser(request, daoContext);
    if (user == null) {
      throw new IllegalStateException("user can not be null");
    }
    StringBuffer jsonObject = new StringBuffer();
    JSONObject params = null;
    Map<String, String[]> paramsMap = null;
    String url = request.getRequestURL().toString();
    jsonObject.append("request:");
    jsonObject.append(url).append("\n");
    String requestBody = null;
    if (!ConfigFileContext.HTTPMethod.GET.name().equals(request.getMethod())) {
      paramsMap = request.getParameterMap();
      if (paramsMap.size() > 0) {
        params = new JSONObject();
        for (Map.Entry<String, String[]> entry : paramsMap.entrySet()) {
          if (entry.getValue().length == 1) {
            params.put(entry.getKey(), entry.getValue()[0]);
          } else {
            params.put(entry.getKey(), entry.getValue());
          }
        }
        jsonObject.append("params:").append("\n");
        jsonObject.append(params.toString(1)).append("\n");
      }

      try (ServletInputStream input = request.getInputStream()) {
        if (!input.isFinished()) {
          requestBody = IOUtils.toString(input, TisUTF8.get());
          jsonObject.append("body:").append("\n");
          jsonObject.append(requestBody).append("\n");
        }
      }
    }
    Context context = new MockContext();
    jsonObject.append("\n=================================");
    jsonObject.append("response:").append("\n");
    jsonObject.append(AjaxValve.buildResultStruct(context));
    log.setTabName(proxy.getActionName());
    log.setUsrId(user.getId());
    log.setUsrName(user.getName());
    log.setOpDesc(jsonObject.toString());
    operationLogDAO.insertSelective(log);
    return result;
  }

  @Autowired
  public void setOperationLogDAO(IOperationLogDAO operationLogDAO) {
    this.operationLogDAO = operationLogDAO;
  }

  @Autowired
  public final void setRunContextGetter(RunContextGetter daoContextGetter) {
    this.daoContext = daoContextGetter.get();
  }
}

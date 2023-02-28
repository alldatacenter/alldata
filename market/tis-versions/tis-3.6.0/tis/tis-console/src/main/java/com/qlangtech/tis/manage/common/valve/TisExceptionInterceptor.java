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

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.interceptor.MethodFilterInterceptor;
import com.qlangtech.tis.lang.TisException;
import com.qlangtech.tis.manage.common.MockContext;
import com.qlangtech.tis.manage.common.TisActionMapper;
import com.qlangtech.tis.manage.spring.aop.AuthorityCheckAdvice;
import com.qlangtech.tis.order.center.IParamContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 拦截系统异常，以控制页面友好
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年1月23日 下午2:33:00
 */
public class TisExceptionInterceptor extends MethodFilterInterceptor {

  private static final long serialVersionUID = 1L;

  private static final Logger logger = LoggerFactory.getLogger(TisExceptionInterceptor.class);

  private PlatformTransactionManager transactionManager;

  @Autowired
  public void setTransactionManager(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  @Override
  protected boolean applyInterceptor(ActionInvocation invocation) {
    return AuthorityCheckAdvice.inNotForwardProcess();
  }

  @Override
  protected String doIntercept(ActionInvocation invocation) throws Exception {
    HttpServletResponse response = ServletActionContext.getResponse();
    HttpServletRequest request = ServletActionContext.getRequest();

    boolean disableTransaction = Boolean.parseBoolean(request.getParameter(IParamContext.KEY_REQUEST_DISABLE_TRANSACTION));
    TransactionStatus status = null;
    if (!disableTransaction) {
      status = transactionManager.getTransaction(new DefaultTransactionDefinition());
    }
    ActionProxy proxy = invocation.getProxy();
    AjaxValve.ActionExecResult execResult = null;
    try {

      if (disableTransaction) {
        return invocation.invoke();
      } else {
        invocation.getInvocationContext().put(TransactionStatus.class.getSimpleName(), status);
        final String result = invocation.invoke();
        execResult = MockContext.getActionExecResult();
        // 一定要invoke之后再执行
        if (!execResult.isSuccess()) {
          // 业务失败也要回滚
          transactionManager.rollback(status);
          return result;
        }
        if (!status.isCompleted()) {
          transactionManager.commit(status);
        }
        return result;
      }

    } catch (Throwable e) {
      logger.error(e.getMessage(), e);
      if (!disableTransaction && !status.isCompleted()) {
        transactionManager.rollback(status);
      }

      if (StringUtils.endsWith(proxy.getNamespace(), TisActionMapper.ACTION_TOKEN)) {
        List<Object> errors = new ArrayList<>();
        errors.add("服务端发生异常，请联系系统管理员");
        TisException.ErrMsg errMsg = TisException.getErrMsg(e);
        errors.add(errMsg.writeLogErr());
        AjaxValve.writeInfo2Client(() -> false, response, false
          , errors, Collections.emptyList(), Collections.emptyList(), null);
        return Action.NONE;
      } else {
        throw e;
      }
    }
  }
}

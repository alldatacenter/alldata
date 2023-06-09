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
import com.qlangtech.tis.manage.common.AppDomainInfo;
import com.qlangtech.tis.manage.common.CheckAppDomainExistValve;
import com.qlangtech.tis.manage.common.RunContextGetter;
import com.qlangtech.tis.manage.spring.aop.AuthorityCheckAdvice;
import com.qlangtech.tis.pubhook.common.Nullable;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import com.qlangtech.tis.runtime.module.action.BasicModule;
import com.qlangtech.tis.runtime.module.action.BasicModule.Rundata;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 校验当前应用是否选择了appdomain
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-4-26
 */
public class AppDomainSelectedCheckValve extends MethodFilterInterceptor {

  private static final long serialVersionUID = -6852248426374953157L;

  private static final Map<String, Collection<RunEnvironment>> include_urls = new HashMap<String, Collection<RunEnvironment>>();

  private static final Collection<RunEnvironment> NULL_RUNTIME = Collections.emptyList();

  static {
    // 以下罗列中表示该应用不支持的环境
    include_urls.put("/runtime/index_query", NULL_RUNTIME);
    include_urls.put("/runtime/hsf_monitor", NULL_RUNTIME);
    include_urls.put("/runtime/jarcontent/snapshotset", NULL_RUNTIME);
    // include_urls.add("/changedomain");
    // include_urls.put("/queryresponse",
    // Arrays.asList(RunEnvironment.DAILY,
    // RunEnvironment.READY));
    include_urls.put("/realtimelog", NULL_RUNTIME);
    include_urls.put("/runtime/cluster_state", NULL_RUNTIME);
    // include_urls.put("/zklockview", Arrays.asList(RunEnvironment.DAILY));
    include_urls.put("/runtime/zklockview", NULL_RUNTIME);
    include_urls.put("/runtime/jarcontent/snapshotlist", NULL_RUNTIME);
    // include_urls.put("/publishZookeeperWrapper", Arrays.asList(
    // RunEnvironment.ONLINE, RunEnvironment.READY));
    // http://l.admin.taobao.org/runtime/hdfs_view.htm
    include_urls.put("/runtime/hdfs_view", NULL_RUNTIME);
    // include_urls.put("/jarcontent/grouplist", Arrays.asList(
    // RunEnvironment.ONLINE, RunEnvironment.READY));
    // include_urls.put("/launchdumpandbuildindex", Arrays.asList(
    // RunEnvironment.ONLINE, RunEnvironment.READY));
    include_urls.put("/runtime/jarcontent/grouplist", NULL_RUNTIME);
    include_urls.put("/runtime/launchdumpandbuildindex", NULL_RUNTIME);
    // http://l.admin.taobao.org/trigger/triggermonitor.htm
    include_urls.put("/runtime/triggermonitor", NULL_RUNTIME);
    // include_urls.put("/hdfsuserlist",
    // Arrays.asList(RunEnvironment.ONLINE,
    // RunEnvironment.READY));
    include_urls.put("/runtime/schema_manage", NULL_RUNTIME);
    include_urls.put("/runtime/server_config_view", NULL_RUNTIME);
    // ▼▼▼coredefine
    include_urls.put("/coredefine/coredefine", NULL_RUNTIME);
    include_urls.put("/coredefine/coredefine_step1", NULL_RUNTIME);
    include_urls.put("/coredefine/corenodemanage", NULL_RUNTIME);
    include_urls.put("/trigger/app_list", NULL_RUNTIME);
    include_urls.put("/trigger/buildindexmonitor", NULL_RUNTIME);
    include_urls.put("/coredefine/cluster_servers_view", NULL_RUNTIME);
    // ▲▲▲
    include_urls.put("/trigger/task_list", NULL_RUNTIME);
    include_urls.put("/runtime/app_trigger_view", NULL_RUNTIME);
  }

  private Map<String, String> specialForward = new HashMap<String, String>();

  public void setSpecialForward(Map<String, String> specialForward) {
    this.specialForward = specialForward;
  }

  private RunContextGetter daoContextGetter;

  @Autowired
  public final void setRunContextGetter(RunContextGetter daoContextGetter) {
    this.daoContextGetter = daoContextGetter;
  }

  @Override
  protected boolean applyInterceptor(ActionInvocation invocation) {
    return AuthorityCheckAdvice.inNotForwardProcess();
  }

  @Override
  protected String doIntercept(ActionInvocation invocation) throws Exception {


    final ActionProxy proxy = invocation.getProxy();
    if ("control".equals(StringUtils.split(proxy.getNamespace(), "#")[1])) {
      return invocation.invoke();
    }
    final Rundata rundata = BasicModule.getRundataInstance();
    AppDomainInfo domain = CheckAppDomainExistValve.getAppDomain(daoContextGetter.get());
    final String actionTarget = getActionTarget(proxy);
    if (!include_urls.containsKey(actionTarget)) {
      // 不在校验范围之内
      return invocation.invoke();
    }
    final String specialTarget = specialForward.get(actionTarget);
    boolean sensitiveRuntime = true;
    try {
      if (StringUtils.isNotBlank(specialTarget)) {
        sensitiveRuntime = "true".equalsIgnoreCase(StringUtils.substringAfter(specialTarget, ","));
      }
    } catch (Throwable e) {
    }
    if (isInvalidDomain((BasicModule) proxy.getAction(), domain, sensitiveRuntime)) {
      if (StringUtils.isNotBlank(specialTarget)) {
        rundata.forwardTo(StringUtils.substringBefore(specialTarget, ","));
      } else {
        rundata.forwardTo("appdomainhasnotselected");
        return Action.NONE;
      }
      // context.breakPipeline(0);
      return invocation.invoke();
      // return;
    }
    Collection<RunEnvironment> runtime = include_urls.get(actionTarget);
    if (runtime.contains(domain.getRunEnvironment())) {
      // 跳转到该应用是不能被使用的，不支持 该环境的应用
      rundata.forwardTo("environmentunuseable");
    }
    return invocation.invoke();
  }

  private String getActionTarget(ActionProxy proxy) {
    // ActionProxy proxy = invocation.getProxy();
    return StringUtils.split(proxy.getNamespace(), "#")[0] + "/" + proxy.getActionName();
  }

  protected boolean isInvalidDomain(BasicModule basicAction, AppDomainInfo domain, boolean sensitiveRuntime) {
    if (!basicAction.isAppNameAware()) {
      return (domain instanceof Nullable);
    }
    return (domain instanceof Nullable) || domain instanceof AppDomainInfo.EnvironmentAppDomainInfo;
  }
}

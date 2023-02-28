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
package com.qlangtech.tis.manage.common;

import com.qlangtech.tis.manage.biz.dal.dao.IApplicationDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.biz.dal.pojo.ApplicationCriteria;
import com.qlangtech.tis.manage.common.DefaultFilter.AppAndRuntime;
import com.qlangtech.tis.pubhook.common.Nullable;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import org.apache.commons.lang.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

/**
 * 校验用户当前 应用是否选择了
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class CheckAppDomainExistValve {

  // private static final Pattern p = Pattern
  // .compile("bizid(\\d+)appid(\\d+)run(\\d+)");
  // app.getProjectName()
  // + "_run" + form.getRunEnviron()
  @Autowired
  private HttpServletRequest request;

  // @Autowired
  // private URIBrokerService uriService;
  //
  // public static final String CHANGE_DOMAIN_TARGET = "/changedomain";
  //
  // public void invoke(PipelineContext pipelineContext) throws Exception {
  //
  // AppDomainInfo appDomain = getAppDomain((RunContext) null);
  //
  // if (appDomain == null) {
  // TurbineRunData rundata = TurbineUtil.getTurbineRunData(request);
  //
  // if (!StringUtils.equalsIgnoreCase(rundata.getTarget(),
  // CHANGE_DOMAIN_TARGET)) {
  // String jumpTo = BasicModule.getBroker(uriService).setTarget(
  // CHANGE_DOMAIN_TARGET).toString();
  // rundata.setRedirectLocation(jumpTo
  // + "?_fm.ch._0.g="
  // + URLEncoder.encode(String.valueOf(request
  // .getRequestURL()), BasicModule.getEncode()));
  // pipelineContext.breakPipeline(Pipeline.TOP_LABEL);
  // }
  //
  // }
  //
  // pipelineContext.invokeNext();
  // }
  // public static AppDomainInfo getAppDomain(RunContext context) {
  // return getAppDomain(context.getApplicationDAO());
  // }
  public static AppDomainInfo getAppDomain(RunContext context) {
    HttpServletRequest request = ServletActionContext.getRequest();
    return getAppDomain(request, context);
  }

  public static AppDomainInfo getAppDomain(HttpServletRequest request, RunContext context) {
    // Assert.assertNotNull(applicationDAO);
    AppDomainInfo domain = (AppDomainInfo) request.getAttribute(ActionTool.REQUEST_DOMAIN_KEY);
    if (domain != null) {
      return domain;
    }
    // Integer bizid = null;
    // Integer appid = null;
    AppDomainInfo appDomain = null;
    // Cookie cookie = getCookie(request,
    // ChangeDomainAction.COOKIE_SELECT_APP);
    // if (cookie == null) {
    // // domain = new NullAppDomainInfo(applicationDAO);
    // domain = AppDomainInfo.createAppNotAware(getRuntime());
    // request.setAttribute(ActionTool.REQUEST_DOMAIN_KEY, domain);
    // return domain;
    // }
    // Matcher match = p2.matcher(cookie.getValue());
    // .getRuntime(request);
    AppAndRuntime environment = DefaultFilter.getAppAndRuntime();
    if (environment == null) {
      domain = AppDomainInfo.createAppNotAware(DefaultFilter.getRuntime());
      request.setAttribute(ActionTool.REQUEST_DOMAIN_KEY, domain);
      return domain;
    }
    try {
      if (StringUtils.isEmpty(environment.getAppName())) {
        // 只选择了环境 参数
        // appDomain = new AppDomainInfo(-1, -1, Integer
        // .parseInt(match.group(2)), context);
        appDomain = AppDomainInfo.createAppNotAware(environment.getRuntime());
      } else {
        appDomain = queryApplication(request, context, environment.getAppName(), environment.getRuntime());
        if (appDomain == null) {
          Application app = new Application();
          app.setProjectName(environment.getAppName());
          appDomain = new AppDomainInfo(0, 0, environment.getRuntime(), app);
        }
      }
    } catch (Exception e) {
      // return new NullAppDomainInfo(context.getApplicationDAO());
      throw new IllegalStateException(e);
    }
    if (appDomain == null) {
      appDomain = CheckAppDomainExistValve.createNull();
    }
    request.setAttribute(ActionTool.REQUEST_DOMAIN_KEY, appDomain);
    return appDomain;
  }

  public static AppDomainInfo queryApplication(HttpServletRequest request, RunContext context, final String appname, RunEnvironment runtime) {
    if (true) {
      ApplicationCriteria query = new ApplicationCriteria();
      query.createCriteria().andProjectNameEqualTo(appname);
      AppDomainInfo appDomain = null;
      for (Application app : context.getApplicationDAO().selectByExample(query)) {
        // Integer bizid, Integer appid, RunEnvironment runEnvironment, Application app
        appDomain = new AppDomainInfo(app.getDptId(), app.getAppId(), // getRuntime(match),
          runtime, app);
        // appDomain.setAppName(appname);
        break;
      }
      return appDomain;
    }
    return null;
    // // ApplicationCriteria criteria = new ApplicationCriteria();
    // // criteria.createCriteria().andProjectNameEqualTo(
    // // // match.group(1)
    // // appname);// .andNotDelete();
    // AppDomainInfo appDomain = null;
    // IAppsFetcher appFetcher = UserUtils.getAppsFetcher(request, context);
    //
    // List<Application> applist = appFetcher.getApps(new CriteriaSetter() {
    // @Override
    // public void set(Criteria criteria) {
    // criteria.andProjectNameEqualTo(appname);
    // }
    // });
    //
    // // List<Application> applist = applicationDAO.selectByExample(criteria,
    // // 1,
    // // 100);
    //
    // // AppsFetcher.create();
    //
    // for (Application app : applist) {
    // // appDomain = new AppDomainInfo(app, Integer
    // // .parseInt(match.group(2)), applicationDAO);
    //
    // // 如果应用的部门为空则说明不是一个合法的部门
    // if (app.getDptId() == null || app.getDptId() < 1) {
    // return CheckAppDomainExistValve.createNull();
    // }
    //
    // appDomain = new AppDomainInfo(app.getDptId(), app.getAppId(), runtime,
    // // getRuntime(match),
    // context);
    //
    // appDomain.setAutoDeploy(app.getIsAutoDeploy());
    // break;
    // }
    // return appDomain;
  }

  private static final AppDomainInfo NULL = new NullAppDomainInfo(null);

  public static AppDomainInfo createNull() {
    return NULL;
  }

  private static class NullAppDomainInfo extends AppDomainInfo implements Nullable {

    // Integer bizid, Integer appid, RunEnvironment runEnvironment, Application app
    private NullAppDomainInfo(IApplicationDAO applicationDAO) {
      super(Integer.MAX_VALUE, Integer.MAX_VALUE, RunEnvironment.DAILY, null);
    }

    @Override
    protected void judgeNull(Application application) {
    }

    @Override
    public Integer getAppid() {
      return 0;
    }

    @Override
    public String getAppName() {
      return StringUtils.EMPTY;
    }

    @Override
    public Integer getDptid() {
      return -1;
    }

    @Override
    public RunEnvironment getRunEnvironment() {
      // throw new UnsupportedOperationException();
      return RunEnvironment.DAILY;
    }
  }
}

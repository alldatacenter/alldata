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

import com.qlangtech.tis.pubhook.common.RunEnvironment;
import com.qlangtech.tis.runtime.module.action.ChangeDomainAction;
import junit.framework.Assert;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public final class DefaultFilter implements Filter {

  private static ThreadLocal<ServletResponse> responseLocal = new ThreadLocal<ServletResponse>();

  private static ThreadLocal<AppAndRuntime> appAndRuntimeLocal = new ThreadLocal<AppAndRuntime>();

  private static ThreadLocal<TISHttpServletRequestWrapper> requestLocal = new ThreadLocal<TISHttpServletRequestWrapper>();

  // public static void setThreadRequest(AdapterHttpRequest request) {
  // requestLocal.set(request);
  // }
  public static AppAndRuntime getAppAndRuntime() {
    return appAndRuntimeLocal.get();
  }

  public static void setAppAndRuntime(AppAndRuntime appAndRuntime) {
    appAndRuntimeLocal.set(appAndRuntime);
  }

  public static ServletResponse getRespone() {
    // ServletActionContext.getResponse();
    return responseLocal.get();
  }

  // private TerminatorEagleEyeFilter eagleEyeFilter;
  public static TISHttpServletRequestWrapper getReqeust() {
    TISHttpServletRequestWrapper request = requestLocal.get();
    Assert.assertNotNull("request has not been set in local thread", request);
    return request;
  }

  // private static final Pattern p2 = Pattern.compile("(.*?)_run(\\d+)");
  private static AppAndRuntime getRuntime(TISHttpServletRequestWrapper request) {
    final String key = "request" + ChangeDomainAction.COOKIE_SELECT_APP;
    if (request.getAttribute(key) == null) {
      AppAndRuntime appAndRuntime = new AppAndRuntime();
      String appName = StringUtils.defaultString(request.getHeader("appname"), request.getParameter("appname"));
      // if (cookie == null) {
      if (StringUtils.isBlank(appName)) {
        // RunEnvironment.getSysEnvironment();//
        appAndRuntime.runtime = RunEnvironment.getSysRuntime();
        // ManageUtils.isDevelopMode()
        // ?
        // RunEnvironment.DAILY
        // :
        // RunEnvironment.ONLINE;
        request.setAttribute(key, appAndRuntime);
        // 只有预发和线上的可能了
        return appAndRuntime;
      }
      // Matcher match = p2.matcher(cookie.getValue());
      // if (match.matches()) {
      appAndRuntime.appName = appName;
      // RunEnvironment.getSysEnvironment();//
      appAndRuntime.runtime = RunEnvironment.getSysRuntime();
      // RunEnvironment.getEnum(Short.parseShort(match.group(2)));
      if (!ManageUtils.isDaily() && appAndRuntime.runtime != RunEnvironment.DAILY) {
        request.setAttribute(key, appAndRuntime);
        // 只有预发和线上的可能了
        return appAndRuntime;
      }
      // }
      appAndRuntime.runtime = getRuntime();
      request.setAttribute(key, appAndRuntime);
      return appAndRuntime;
    }
    return (AppAndRuntime) request.getAttribute(key);
  }

  public static RunEnvironment getRuntime() {
    // RunEnvironment.getSysEnvironment();
    return RunEnvironment.getSysRuntime();
  }

  public static class AppAndRuntime {

    private String appName;

    private RunEnvironment runtime;

    public void setAppName(String appName) {
      this.appName = appName;
    }

    public void setRuntime(RunEnvironment runtime) {
      this.runtime = runtime;
    }

    public String getAppName() {
      return appName;
    }

    public RunEnvironment getRuntime() {
      return runtime;
    }
  }

  // public static Cookie getCookie(HttpServletRequest request, String
  // cookieName) {
  // Cookie[] cookies = request.getCookies();
  // if (cookies == null) {
  // return null;
  // }
  // for (Cookie c : cookies) {
  // if (StringUtils.equals(c.getName(), cookieName)) {
  // return c;
  // }
  // }
  // return null;
  // }
  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(final ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    // long start = System.currentTimeMillis();
    try {
      request.setCharacterEncoding(TisUTF8.getName());
      response.setCharacterEncoding(TisUTF8.getName());
      final TISHttpServletRequestWrapper wrapperRequest = new TISHttpServletRequestWrapper((HttpServletRequest) request);
      responseLocal.set(response);
      requestLocal.set(wrapperRequest);
      appAndRuntimeLocal.set(getRuntime(wrapperRequest));
      if (ManageUtils.isDaily()) {
        // com.alibaba.hecla.acl.dataobject.SysUser user = new
        // com.alibaba.hecla.acl.dataobject.SysUser();
        // user.setId(18097);
        // user.setName("default");
        // user.setLoginTime(new Date((new Date()).getTime() - 10000));
        // securityContext.setUser(user);
      } else {
        // securityContext.setUser(HeclaLoginValve
        // .getSysUser(wrapperRequest));
      }
      // SecurityContextHolder.setContext(securityContext);
      chain.doFilter(wrapperRequest, response);
    } finally {
      // responseLocal.set(null);
      // requestLocal.set(null);
      // System.out.println("consume:"
      // + (System.currentTimeMillis() - start));
    }
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    //  AbstractTisCloudSolrClient.initHashcodeRouter();
  }
}

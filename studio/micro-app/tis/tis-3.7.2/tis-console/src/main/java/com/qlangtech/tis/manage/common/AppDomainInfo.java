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

import com.qlangtech.tis.manage.biz.dal.pojo.AppType;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class AppDomainInfo {

  private final Integer dptid;

  private final Integer appid;

  private final RunEnvironment runEnvironment;

  // private final RunContext context;
  private final Application app;

  // private String appName;
  private boolean isAutoDeploy;

  public boolean isAutoDeploy() {
    return isAutoDeploy;
  }

  public void setAutoDeploy(boolean isAutoDeploy) {
    this.isAutoDeploy = isAutoDeploy;
  }

  public AppDomainInfo(Integer bizid, Integer appid, RunEnvironment runEnvironment, Application app) {
    super();
    judgeNull(app);
    this.dptid = bizid;
    this.appid = appid;
    this.runEnvironment = runEnvironment;
    // this.context = context;
    this.app = app;
    // return application.getProjectName();
  }

  public final AppType getAppType() {
    return this.getApp().parseAppType();
  }

  public Application getApp() {
    if (this.app == null) {
      throw new IllegalStateException("app can not be null");
    }
    return app;
  }

  public String getAppName() {
    return this.app.getProjectName();
  }


  /**
   * 创建于应用无关的当前环境
   *
   * @param runEnvironment
   * @return
   */
  public static AppDomainInfo createAppNotAware(RunEnvironment runEnvironment) {
    return new EnvironmentAppDomainInfo(runEnvironment);
  }

  public static class EnvironmentAppDomainInfo extends AppDomainInfo {

    public EnvironmentAppDomainInfo(RunEnvironment runEnvironment) {
      super(-1, -1, runEnvironment, (Application) null);
    }

    @Override
    public String getAppName() {
      return StringUtils.EMPTY;
    }

    @Override
    protected void judgeNull(Application context) {
    }
  }

  protected void judgeNull(Application app) {
    // context) {
    if (app == null) {
      throw new IllegalArgumentException("app can not be null");
    }
  }

  public static String getRunEnvir(int runEnvironment) {
    return RunEnvironment.getEnum((short) runEnvironment).getDescribe();
  }

  public RunEnvironment getRunEnvironment() {
    return runEnvironment;
  }

  // public Integer getBizid() {
  // return dptid;
  // }
  public Integer getDptid() {
    return dptid;
  }

  public Integer getAppid() {
    return appid;
  }

  @Override
  public String toString() {
    return "{" +
      "appid=" + appid +
      ", runEnvironment=" + runEnvironment +
      '}';
  }
}

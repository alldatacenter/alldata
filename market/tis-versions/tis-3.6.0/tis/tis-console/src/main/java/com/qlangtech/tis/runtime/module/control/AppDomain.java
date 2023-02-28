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
package com.qlangtech.tis.runtime.module.control;

import com.alibaba.citrus.turbine.Context;
import org.apache.commons.lang.StringUtils;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.biz.dal.pojo.Department;
import com.qlangtech.tis.manage.common.AppDomainInfo;
import com.qlangtech.tis.manage.common.CheckAppDomainExistValve;
import com.qlangtech.tis.pubhook.common.Nullable;
import com.qlangtech.tis.runtime.module.action.BasicModule;
import com.qlangtech.tis.runtime.module.action.ChangeDomainAction;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class AppDomain extends BasicModule {

    /**
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param
     */
    public AppDomain() {
        super(StringUtils.EMPTY);
    }

    public void execute(Context context) throws Exception {
        AppDomainInfo appDomain = getAppDomain();
        if (appDomain == null) {
            // 跳转到指定上下文的页面
            // .withTarget("changedomain");
            getRundataInstance().redirectTo("changedomain");
            return;
        }
        Department department = null;
        // final Department department = this.getDepartmentDAO().loadFromWriteDB(
        // appDomain.getDptid());
        // Application app = this.getApplicationDAO().loadFromWriteDB(
        // appDomain.getAppid());
        Application app = new Application();
        app.setProjectName(appDomain.getAppName());
        // 校验是否选择了当前应用？
        if ((appDomain instanceof Nullable) || !shallSelectApp(department, app)) {
            appDomain = CheckAppDomainExistValve.createNull();
            context.put(ChangeDomainAction.SELECT_APP_NAME, getNotSelectDomainCaption());
        } else {
            boolean shallnotShowEnvironment = (context.get("shallnotShowEnvironment") != null) && (Boolean) context.get("shallnotShowEnvironment");
            context.put(ChangeDomainAction.SELECT_APP_NAME, getAppDesc(appDomain, department, app, shallnotShowEnvironment));
        }
        context.put("dptid", appDomain.getDptid());
        context.put("appid", appDomain.getAppid());
        context.put("runid", appDomain.getRunEnvironment().getId());
    }

    protected String getNotSelectDomainCaption() {
        return "您尚未选择应用";
    }

    protected boolean shallSelectApp(com.qlangtech.tis.manage.biz.dal.pojo.Department department, Application app) {
        // return department != null && app != null;
        return true;
    }

    protected String getAppDesc(AppDomainInfo appDomain, Department department, Application app, boolean shallnotShowEnvironment) {
        return getAppDescribe(app);
    }

    public static String getAppDescribe(Application app) {
        return app.getProjectName();
    // if (StringUtils.contains(app.getDptName(), "-")) {
    // return StringUtils.substringAfterLast(app.getDptName(), "-") + "["
    // + app.getProjectName() + "]";
    // } else {
    // return app.getDptName() + "[" + app.getProjectName() + "]";
    // }
    }
}

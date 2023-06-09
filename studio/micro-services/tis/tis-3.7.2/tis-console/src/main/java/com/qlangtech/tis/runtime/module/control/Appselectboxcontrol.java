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

import java.util.List;
import com.alibaba.citrus.turbine.Context;
import org.apache.commons.lang.StringUtils;
import com.qlangtech.tis.manage.common.AppDomainInfo;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.pubhook.common.Nullable;
import com.qlangtech.tis.runtime.module.action.BasicModule;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-11-19
 */
public class Appselectboxcontrol extends BasicModule {

    private static final long serialVersionUID = 1L;

    static final String key = Appselectboxcontrol.class.getName() + ".AppOptionsList";

    private String contextid;

    private boolean maxMatch = false;

    private boolean onlySelectDpt = false;

    private Integer selectDptId;

    public boolean isOnlySelectDpt() {
        return onlySelectDpt;
    }

    public void setOnlySelectDpt(boolean onlySelectDpt) {
        this.onlySelectDpt = onlySelectDpt;
    }

    public String getContextid() {
        return StringUtils.trimToEmpty(contextid);
    }

    public void setContextid(String contextid) {
        this.contextid = contextid;
    }

    public Integer getSelectDptId() {
        return selectDptId;
    }

    public void setSelectDptId(Integer selectDptId) {
        this.selectDptId = selectDptId;
    }

    public void execute(Context context) throws Exception {
        // ${contextid}
        // if (context.get("contextid") == null) {
        // context.put("contextid", StringUtils.EMPTY);
        // }
        AppOptionsList optionslist = (AppOptionsList) this.getRequest().getAttribute(key);
        if (optionslist == null) {
            final List<Option> bizlist = this.getBizLineList();
            List<Option> applist = null;
            AppDomainInfo domain = this.getAppDomain();
            if (!(domain instanceof Nullable)) {
                // if (bizid != null) {
                applist = this.getAppList(domain.getDptid());
            // }
            }
            optionslist = new AppOptionsList(bizlist, applist);
            this.getRequest().setAttribute(key, optionslist);
        }
        context.put("bizlinelist", optionslist.bizlinelist);
        context.put("applist", optionslist.applist);
    }

    private static class AppOptionsList {

        private final List<Option> bizlinelist;

        private final List<Option> applist;

        public AppOptionsList(List<Option> bizlinelist, List<Option> applist) {
            super();
            this.bizlinelist = bizlinelist;
            this.applist = applist;
        }
    }
}

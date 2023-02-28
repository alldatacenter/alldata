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
import com.qlangtech.tis.runtime.module.action.BasicModule;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-5-11
 */
public class Appselect extends BasicModule {

    private static final long serialVersionUID = 1L;

    // 是否是从Daily中索取资源
    private boolean fromDaily = false;

    // private boolean maxMatch = false;
    public // @Param("bizid") Integer bizid,
    void execute(Context context) throws Exception {
    }

    public String getFromSymbol() {
        return fromDaily ? "app_relevant_from_daily_action" : "change_domain_action";
    }

    @Override
    public boolean isEnableDomainView() {
        return false;
    }

    public boolean isFromDaily() {
        return fromDaily;
    }

    // public boolean isMaxMatch() {
    // return maxMatch;
    // }
    public void setFromDaily(boolean fromDaily) {
        this.fromDaily = fromDaily;
    }
}

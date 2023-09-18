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
package com.qlangtech.tis.manage.util;

import com.qlangtech.tis.pubhook.common.IPreTriggerProcess;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年11月9日 下午3:04:03
 */
public class TisPreTriggerProcess implements IPreTriggerProcess {

    // private static final AdapterHttpRequest request;
    //
    // static {
    // request = new AdapterHttpRequest() {
    // @Override
    // public Cookie[] getCookies() {
    // int runtimeCode = ManageUtils.isDevelopMode() ? RunEnvironment.DAILY
    // .getId() : RunEnvironment.ONLINE.getId();
    // Cookie cookie = new Cookie(
    // ChangeDomainAction.COOKIE_SELECT_APP, "search4xxx_run"
    // + runtimeCode);
    // return new Cookie[] { cookie };
    // }
    // };
    // }
    @Override
    public void process() throws Exception {
    // DefaultFilter.setThreadRequest(request);
    }
}

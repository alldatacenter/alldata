/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.web.start;

/**
 * TIS 子模块枚举
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-08-29 16:29
 **/
public enum TisSubModule {
    WEB_START("web-start", 0),
    TIS_ASSEMBLE("tis-assemble", 1),
    ZEPPELIN("tis-zeppelin", "/next", 2),
    TIS_CONSOLE("tjs", 0),
    TIS_COLLECT("tis-collect", 0);

    public final String moduleName;
    public final String servletContext;
    public final int portOffset;

    private TisSubModule(String name, String servletContext, int portOffset) {
        this.moduleName = name;
        this.servletContext = servletContext;
        this.portOffset = TisAppLaunch.isTestMock() ? portOffset : 0;
    }

    private TisSubModule(String name, int portOffset) {
        this(name, "/" + name, portOffset);
//        this.moduleName = name;
//        this.servletContext = ;
//        this.portOffset = TisAppLaunch.isTestMock() ? portOffset : 0;
    }

    public int getLaunchPort() {
        TisAppLaunch i = TisAppLaunch.get();
        return i.launchPort + this.portOffset;
    }
}

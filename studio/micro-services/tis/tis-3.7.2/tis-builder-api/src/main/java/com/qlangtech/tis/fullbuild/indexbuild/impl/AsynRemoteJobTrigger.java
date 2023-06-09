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

package com.qlangtech.tis.fullbuild.indexbuild.impl;

import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;

/**
 * 异步任务
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-02 18:03
 **/
public abstract class AsynRemoteJobTrigger implements IRemoteTaskTrigger {
    private final String jobName;

    public AsynRemoteJobTrigger(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public String getTaskName() {
        return this.jobName;
    }

    @Override
    public final boolean isAsyn() {
        return true;
    }

    @Override
    public String getAsynJobName() {
        return this.jobName;
    }
}

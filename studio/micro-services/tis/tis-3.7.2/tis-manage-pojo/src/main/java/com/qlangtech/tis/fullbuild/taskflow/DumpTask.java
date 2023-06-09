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

package com.qlangtech.tis.fullbuild.taskflow;

import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.exec.ExecChainContextUtils;
import com.qlangtech.tis.fullbuild.indexbuild.DftTabPartition;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.fullbuild.phasestatus.impl.AbstractChildProcessStatus;
import com.qlangtech.tis.sql.parser.TabPartitions;

import java.util.Collections;
import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-15 22:15
 **/
public class DumpTask extends DataflowTask {
    private IRemoteTaskTrigger jobTrigger;
    private AbstractChildProcessStatus taskStatus;

    public DumpTask(IRemoteTaskTrigger jobTrigger, AbstractChildProcessStatus taskStatus) {
        super(jobTrigger.getTaskName());
        this.jobTrigger = jobTrigger;
        this.taskStatus = taskStatus;
    }

    public static DataflowTask createDumpTask(
            IRemoteTaskTrigger jobTrigger, AbstractChildProcessStatus taskStatus) {
        taskStatus.setWaiting(true);
        return new DumpTask(jobTrigger, taskStatus);
    }

    @Override
    public void run() throws Exception {
        taskStatus.setWaiting(false);
        boolean faild = true;
        try {
            this.jobTrigger.run();
            faild = false;
        } finally {
            if (!this.jobTrigger.isAsyn()) {
                taskStatus.setComplete(true);
                taskStatus.setFaild(faild);
            }
        }
    }

    @Override
    protected Map<String, Boolean> getTaskWorkStatus() {
        return Collections.emptyMap();
    }

    @Override
    public FullbuildPhase phase() {
        return FullbuildPhase.FullDump;
    }

    @Override
    public String getIdentityName() {
        return this.id;
    }
}

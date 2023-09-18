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

package com.qlangtech.tis.exec.datax;

import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.datax.IDataXBatchPost;
import com.qlangtech.tis.datax.IDataxContext;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.plugin.ds.ISelectedTab;

import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-11 11:50
 **/
public class BatchPostDataXWriter extends DataxWriter implements IDataXBatchPost {
    boolean runPass = false;
    // boolean execGetTaskDependencies = false;
    boolean preExecute = false;

    private final List<String> taskDependencies;

    public BatchPostDataXWriter(List<String> taskDependencies) {
        this.taskDependencies = taskDependencies;
    }

    public void verify() {
        verify(true);
    }

    public void verify(boolean hasPass) {
        Assert.assertEquals("runPass must be :" + hasPass, hasPass, runPass);
        Assert.assertTrue("preExecute must be true", preExecute);

    }

    @Override
    public ExecutePhaseRange getPhaseRange() {
        return new ExecutePhaseRange(FullbuildPhase.FullDump, FullbuildPhase.JOIN);
    }

    @Override
    public IRemoteTaskTrigger createPreExecuteTask(IExecChainContext execContext, ISelectedTab tab) {
        return new IRemoteTaskTrigger() {
            @Override
            public String getTaskName() {
                return IDataXBatchPost.getPreExecuteTaskName(tab);
            }

            @Override
            public void run() {
                preExecute = true;
            }
        };
    }

    @Override
    public IRemoteTaskTrigger createPostTask(IExecChainContext execContext, final ISelectedTab tab, DataXCfgGenerator.GenerateCfgs cfgFileNames) {
        return new IRemoteTaskTrigger() {
            @Override
            public String getTaskName() {
                return KEY_POST + tab.getName();
            }

//            @Override
//            public List<String> getTaskDependencies() {
//                execGetTaskDependencies = true;
//                return taskDependencies;
//            }

            @Override
            public void run() {
                runPass = true;
            }
        };
    }

    @Override
    public String getTemplate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IDataxContext getSubTask(Optional<IDataxProcessor.TableMap> tableMap) {
        throw new UnsupportedOperationException();
    }
}

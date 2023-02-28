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
package com.qlangtech.tis.exec;

import com.qlangtech.tis.cloud.ITISCoordinator;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.manage.IBasicAppSource;
import com.qlangtech.tis.order.center.IJoinTaskContext;

import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年12月15日 上午11:48:16
 */
public interface IExecChainContext extends IJoinTaskContext {

    public void addAsynSubJob(AsynSubJob jobName);

    public List<AsynSubJob> getAsynSubJobs();

    public boolean containAsynJob();

    class AsynSubJob {
        public final String jobName;

        public AsynSubJob(String jobName) {
            this.jobName = jobName;
        }
    }

    <T extends IBasicAppSource> T getAppSource();

    ITISCoordinator getZkClient();


    String getPartitionTimestamp();

    // IIndexMetaData getIndexMetaData();

    /**
     * 全量構建流程ID
     *
     * @return
     */
    Integer getWorkflowId();

    String getWorkflowName();

    ITISFileSystem getIndexBuildFileSystem();

//    TableDumpFactory getTableDumpFactory();
//
//    IndexBuilderTriggerFactory getIndexBuilderFactory();

    void rebindLoggingMDCParams();
}

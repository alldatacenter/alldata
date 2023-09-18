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

package com.qlangtech.tis.coredefine.module.action.impl;

import com.qlangtech.tis.config.flink.IFlinkClusterConfig;
import com.qlangtech.tis.coredefine.module.action.IDeploymentDetail;
import com.qlangtech.tis.coredefine.module.action.IFlinkIncrJobStatus;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-25 12:37
 **/
public abstract class FlinkJobDeploymentDetails implements IDeploymentDetail {
    private final IFlinkClusterConfig clusterCfg;
    private final IFlinkIncrJobStatus incrJobStatus;

    public abstract boolean isRunning();

    public FlinkJobDeploymentDetails(IFlinkClusterConfig clusterCfg, IFlinkIncrJobStatus incrJobStatus) {
        this.clusterCfg = clusterCfg;
        this.incrJobStatus = incrJobStatus;
    }

    public IFlinkIncrJobStatus getIncrJobStatus() {
        return this.incrJobStatus;
    }

    public IFlinkClusterConfig getClusterCfg() {
        return this.clusterCfg;
    }

    @Override
    public void accept(IDeploymentDetailVisitor visitor) {
        visitor.visit(this);
    }
}

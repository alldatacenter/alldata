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
package com.qlangtech.tis.realtime.yarn.rpc;

import com.qlangtech.tis.fullbuild.phasestatus.impl.BuildSharedPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus.TableDumpStatus;

/**
 * 增量子节点会实时将自己的状态信息汇报给master节点
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年4月7日
 */
public interface IncrStatusUmbilicalProtocol {

    public static final long versionID = 0L;

    public PingResult ping();

    /**
     * 增量执行子节点向服务端节点发送，子节点执行状态
     *
     * @param upateCounter
     */
    public MasterJob reportStatus(UpdateCounterMap upateCounter);

    /**
     * 增量节点启动的时候，向服务端汇报本地的情况，例如:监听的topic是下的tag是什么等等
     *
     * @param launchReportInfo
     */
    public void nodeLaunchReport(LaunchReportInfo launchReportInfo);

    // /**
    // * 报告查询节点的状态信息
    // * @param upateCounter
    // */
    // public void reportQueryNodeStatus(UpdateCounterMap upateCounter);
    /**
     * 报告表dump状态
     */
    public void reportDumpTableStatus(TableDumpStatus tableDumpStatus);

    /**
     * 报告索引build階段執行狀態
     *
     * @param buildStatus
     */
    public void reportBuildIndexStatus(BuildSharedPhaseStatus buildStatus);
}

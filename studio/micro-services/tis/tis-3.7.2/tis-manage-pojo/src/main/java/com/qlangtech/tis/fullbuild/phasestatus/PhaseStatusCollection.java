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
package com.qlangtech.tis.fullbuild.phasestatus;

import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.fullbuild.phasestatus.impl.BuildPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.IndexBackFlowPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.JoinPhaseStatus;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月17日
 */
public class PhaseStatusCollection implements IPhaseStatusCollection {

    private DumpPhaseStatus dumpPhase;

    private JoinPhaseStatus joinPhase;

    private BuildPhaseStatus buildPhase;

    private IndexBackFlowPhaseStatus indexBackFlowPhaseStatus;
    private ExecutePhaseRange executePhaseRange;

    /**
     * @return
     */
    public boolean isComplete() {
        return (!executePhaseRange.contains(FullbuildPhase.FullDump) || dumpPhase.isComplete())
                && (!executePhaseRange.contains(FullbuildPhase.JOIN) || joinPhase.isComplete())
                && (!executePhaseRange.contains(FullbuildPhase.BUILD) || buildPhase.isComplete())
                && (!executePhaseRange.contains(FullbuildPhase.IndexBackFlow) || indexBackFlowPhaseStatus.isComplete());
    }

    public void flushStatus2Local() {
        if (executePhaseRange.contains(FullbuildPhase.FullDump)) {
            dumpPhase.writeStatus2Local();
        }
        if (executePhaseRange.contains(FullbuildPhase.JOIN)) {
            joinPhase.writeStatus2Local();
        }
        if (executePhaseRange.contains(FullbuildPhase.BUILD)) {
            buildPhase.writeStatus2Local();
        }
        if (executePhaseRange.contains(FullbuildPhase.IndexBackFlow)) {
            indexBackFlowPhaseStatus.writeStatus2Local();
        }
    }

    private Integer taskid;

    public PhaseStatusCollection(Integer taskid, ExecutePhaseRange executePhaseRange) {
        super();
        this.executePhaseRange = executePhaseRange;
        this.taskid = taskid;
        this.dumpPhase = new DumpPhaseStatus(taskid);
        this.joinPhase = new JoinPhaseStatus(taskid);
        this.buildPhase = new BuildPhaseStatus(taskid);
        this.indexBackFlowPhaseStatus = new IndexBackFlowPhaseStatus(taskid);
    }

    public Integer getTaskid() {
        return this.taskid;
    }

    public DumpPhaseStatus getDumpPhase() {
        return this.dumpPhase;
    }

    public void setDumpPhase(DumpPhaseStatus dumpPhase) {
        this.dumpPhase = dumpPhase;
    }

    public JoinPhaseStatus getJoinPhase() {
        return this.joinPhase;
    }

    public void setJoinPhase(JoinPhaseStatus joinPhase) {
        this.joinPhase = joinPhase;
    }

    public BuildPhaseStatus getBuildPhase() {
        return this.buildPhase;
    }

    public IndexBackFlowPhaseStatus getIndexBackFlowPhaseStatus() {
        return this.indexBackFlowPhaseStatus;
    }

    public void setBuildPhase(BuildPhaseStatus buildPhase) {
        this.buildPhase = buildPhase;
    }

    public void setIndexBackFlowPhaseStatus(IndexBackFlowPhaseStatus indexBackFlowPhaseStatus) {
        this.indexBackFlowPhaseStatus = indexBackFlowPhaseStatus;
    }
}

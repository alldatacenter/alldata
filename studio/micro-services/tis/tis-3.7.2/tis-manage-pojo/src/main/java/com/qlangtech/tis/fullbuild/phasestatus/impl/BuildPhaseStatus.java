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
package com.qlangtech.tis.fullbuild.phasestatus.impl;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.collect.Maps;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.fullbuild.phasestatus.IProcessDetailStatus;
import org.apache.commons.lang.StringUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月26日
 */
public class BuildPhaseStatus extends BasicPhaseStatus<BuildSharedPhaseStatus> {

    @JSONField(serialize = false)
    public final Map<String, BuildSharedPhaseStatus> /* sharedName */
    nodeBuildStatus = Maps.newConcurrentMap();

    private final ProcessDetailStatusImpl<BuildSharedPhaseStatus> processDetailStatus;

    public BuildPhaseStatus(int taskid) {
        super(taskid);
        this.processDetailStatus = new ProcessDetailStatusImpl<BuildSharedPhaseStatus>(nodeBuildStatus) {

            @Override
            protected BuildSharedPhaseStatus createMockStatus() {
                BuildSharedPhaseStatus s = new BuildSharedPhaseStatus();
                s.setSharedName(StringUtils.EMPTY);
                s.setWaiting(true);
                return s;
            }
        };
    }

    @Override
    protected FullbuildPhase getPhase() {
        return FullbuildPhase.BUILD;
    }

    @Override
    public boolean isShallOpen() {
        return shallOpenView(nodeBuildStatus.values());
    }

    @Override
    public IProcessDetailStatus<BuildSharedPhaseStatus> getProcessStatus() {
        return this.processDetailStatus;
    }

    /**
     * 取得某个shared build执行任务状态
     *
     * @param sharedName
     * @return
     */
    public BuildSharedPhaseStatus getBuildSharedPhaseStatus(String sharedName) {
        if (StringUtils.isEmpty(sharedName)) {
            throw new IllegalArgumentException("param sharedName can not be null");
        }
        BuildSharedPhaseStatus nBuildStatus = nodeBuildStatus.get(sharedName);
        if (nBuildStatus == null) {
            nBuildStatus = new BuildSharedPhaseStatus();
            nBuildStatus.setSharedName(sharedName);
            nBuildStatus.setTaskid(this.getTaskId());
            nodeBuildStatus.put(sharedName, nBuildStatus);
        }
        return nBuildStatus;
    }

//     boolean writeStatus2Local() {
//        return super.writeStatus2Local();
//    }

    @Override
    protected Collection<BuildSharedPhaseStatus> getChildStatusNode() {
        return this.nodeBuildStatus.values();
    }
}

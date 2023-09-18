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
import com.qlangtech.tis.fullbuild.phasestatus.IChildProcessStatusVisitor;
import com.qlangtech.tis.fullbuild.phasestatus.IProcessDetailStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.IndexBackFlowPhaseStatus.NodeBackflowStatus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 索引回流执行状态
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月17日
 */
public class IndexBackFlowPhaseStatus extends BasicPhaseStatus<NodeBackflowStatus> {

    public static final String KEY_INDEX_BACK_FLOW_STATUS = "index_back_flow_status";
    @JSONField(serialize = false)
    public final Map<String, NodeBackflowStatus> /* nodeName */ nodesStatus = Maps.newConcurrentMap();

    public IndexBackFlowPhaseStatus(int taskid) {
        super(taskid);
    }

    @Override
    protected FullbuildPhase getPhase() {
        return FullbuildPhase.IndexBackFlow;
    }

    @Override
    public boolean isShallOpen() {
        return shallOpenView(nodesStatus.values());
    }

    /**
     * 取得某個节点的索引回流状态對象
     */
    public NodeBackflowStatus getNode(String nodeName) {
        NodeBackflowStatus nodeBackflowStatus = this.nodesStatus.get(nodeName);
        if (nodeBackflowStatus == null) {
            nodeBackflowStatus = new NodeBackflowStatus(nodeName);
            this.nodesStatus.put(nodeName, nodeBackflowStatus);
        }
        return nodeBackflowStatus;
    }

    @Override
    public IProcessDetailStatus<NodeBackflowStatus> getProcessStatus() {
        return new IProcessDetailStatus<NodeBackflowStatus>() {
            @Override
            public Collection<NodeBackflowStatus> getDetails() {
                if (nodesStatus.isEmpty()) {
                    NodeBackflowStatus mock = new NodeBackflowStatus(StringUtils.EMPTY);
                    mock.setWaiting(true);
                    return Collections.singleton(mock);
                }
                return nodesStatus.values();
            }

            @Override
            public int getProcessPercent() {
                int allrow = 0;
                for (NodeBackflowStatus s : nodesStatus.values()) {
                    allrow += s.getAllSize();
                }
                if (allrow < 1) {
                    return 0;
                }
                double weight = 0;
                double percent = 0;
                for (Map.Entry<String, NodeBackflowStatus> entry : nodesStatus.entrySet()) {
                    weight = (entry.getValue().getReaded() * 100) / allrow;
                    percent += entry.getValue().getPercent() * weight;
                }
                return (int) (percent / 100);
            }

            @Override
            public void detailVisit(IChildProcessStatusVisitor visitor) {
                for (NodeBackflowStatus s : nodesStatus.values()) {
                    visitor.visit(s);
                }
            }
        };
    }

    @Override
    protected Collection<NodeBackflowStatus> getChildStatusNode() {
        return nodesStatus.values();
    }

    /**
     * 单个replic的回流执行状态
     */
    public static class NodeBackflowStatus extends AbstractChildProcessStatus {

        private final String nodeName;

        // 单位字节
        int allSize;

        // 已经读了多少字节
        int readed;

        /**
         * @param nodeName
         */
        public NodeBackflowStatus(String nodeName) {
            super();
            this.nodeName = nodeName;
        }

        // public boolean isWaiting() {
        // return waiting;
        // }
        //
        // public void setWaiting(boolean waiting) {
        // this.waiting = waiting;
        // }
        public long getAllSize() {
            return allSize;
        }

        public void setAllSize(int allSize) {
            this.allSize = allSize;
        }

        public long getReaded() {
            return readed;
        }

        public void setReaded(int readed) {
            this.readed = readed;
        }

        @Override
        public String getAll() {
            return FileUtils.byteCountToDisplaySize(this.allSize);
        }

        @Override
        public String getProcessed() {
            return FileUtils.byteCountToDisplaySize(this.readed);
        }

        @Override
        public int getPercent() {
            if (this.allSize < 1) {
                return 0;
            }
            return (int) (((this.readed * 1f) / this.allSize) * 100);
        }

        @Override
        public String getName() {
            return this.nodeName;
        }
    }
}

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

package com.qlangtech.plugins.incr.flink.launch;

import com.qlangtech.tis.config.flink.IFlinkClusterConfig;
import com.qlangtech.tis.coredefine.module.action.IFlinkIncrJobStatus;
import com.qlangtech.tis.coredefine.module.action.impl.FlinkJobDeploymentDetails;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.runtime.execution.ExecutionState;
import org.apache.flink.runtime.rest.messages.job.JobDetailsInfo;
import org.apache.flink.runtime.rest.messages.job.metrics.IOMetricsInfo;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-25 13:09
 **/
public class ExtendFlinkJobDeploymentDetails extends FlinkJobDeploymentDetails {
    private final JobDetailsInfo jobDetailsInfo;


    public ExtendFlinkJobDeploymentDetails(IFlinkClusterConfig clusterCfg, IFlinkIncrJobStatus jobStatus, JobDetailsInfo jobDetailsInfo) {
        super(clusterCfg, jobStatus);
        this.jobDetailsInfo = jobDetailsInfo;
    }

    @Override
    public boolean isRunning() {
        return getJobStatus() == JobStatus.RUNNING;
    }

    public String getJobId() {
        return jobDetailsInfo.getJobId().toHexString();
    }

    public String getName() {
        return jobDetailsInfo.getName();
    }

    public boolean isStoppable() {
        return jobDetailsInfo.isStoppable();
    }

    public boolean isCancelable() {
        return !this.getJobStatus().isTerminalState();
    }

    public JobStatus getJobStatus() {
        return jobDetailsInfo.getJobStatus();
    }

    public String getStatusColor() {
        switch (jobDetailsInfo.getJobStatus()) {
            case FINISHED:
                // 灰色
                return "#858585";
            case FAILED:
            case FAILING:
                return "error";
            case CREATED:
            case INITIALIZING:
                return "success";
            case RUNNING:
                return "processing";
            case SUSPENDED:
            case CANCELED:
            case CANCELLING:
            case RESTARTING:
            case RECONCILING:
                return "warning";
            default:
                throw new IllegalStateException("illegal status:" + jobDetailsInfo.getJobStatus());
        }
    }


    public long getStartTime() {
        return jobDetailsInfo.getStartTime();
    }


    public long getEndTime() {
        return jobDetailsInfo.getEndTime();
    }


    public long getMaxParallelism() {
        return jobDetailsInfo.getMaxParallelism();
    }


    public long getDuration() {
        return jobDetailsInfo.getDuration();
    }


    public long getNow() {
        return jobDetailsInfo.getNow();
    }


    public Map<JobStatus, Long> getTimestamps() {
        return jobDetailsInfo.getTimestamps();
    }


    public Collection<WrapperJobVertexDetailsInfo> getSources() {
        return jobDetailsInfo.getJobVertexInfos().stream()
                .map((i) -> new WrapperJobVertexDetailsInfo(i)).collect(Collectors.toList());
    }


    public List<JobVerticesPerState> getJobVerticesPerState() {
        return jobDetailsInfo.getJobVerticesPerState()
                .entrySet().stream()
                .filter((e) -> e.getValue() > 0)
                .map((e) -> new JobVerticesPerState(WrapperJobVertexDetailsInfo.getExecColor(e.getKey()), e.getValue()))
                .collect(Collectors.toList());
    }

    public static class JobVerticesPerState {
        final String stateColor;
        final int count;

        public JobVerticesPerState(String stateColor, int count) {
            this.stateColor = stateColor;
            this.count = count;
        }

        public String getStateColor() {
            return stateColor;
        }

        public int getCount() {
            return count;
        }
    }


    public static final class WrapperJobVertexDetailsInfo {
        private final JobDetailsInfo.JobVertexDetailsInfo jobVertexDetailsInfo;

        public WrapperJobVertexDetailsInfo(JobDetailsInfo.JobVertexDetailsInfo jobVertexDetailsInfo) {
            this.jobVertexDetailsInfo = jobVertexDetailsInfo;
        }

        @JsonIgnore
        public String getJobVertexId() {
            return jobVertexDetailsInfo.getJobVertexID().toHexString();
        }

        @JsonIgnore
        public String getName() {
            // 避免在页面上显示太长只取60个字符
            return StringUtils.left(jobVertexDetailsInfo.getName(), 60);
        }

        public String getFullName(){
            return jobVertexDetailsInfo.getName();
        }

        @JsonIgnore
        public int getMaxParallelism() {
            return jobVertexDetailsInfo.getMaxParallelism();
        }

        @JsonIgnore
        public int getParallelism() {
            return jobVertexDetailsInfo.getParallelism();
        }

        @JsonIgnore
        public ExecutionState getExecutionState() {
            return jobVertexDetailsInfo.getExecutionState();
        }

        public String getExecutionStateColor() {
            return getExecColor(this.getExecutionState());
        }

        protected static String getExecColor(ExecutionState state) {
            switch (state) {
                case INITIALIZING:
                case RECONCILING:
                case CREATED:
                case DEPLOYING:
                case SCHEDULED:
                    return "success";
                case RUNNING:
                    return "processing";
                case FINISHED:
                    return "default";
                case CANCELED:
                case CANCELING:
                    return "warning";
                case FAILED:
                    return "error";
                default:
                    throw new IllegalStateException("illegal state:" + state);
            }
        }

        @JsonIgnore
        public long getStartTime() {
            return jobVertexDetailsInfo.getStartTime();
        }

        @JsonIgnore
        public long getEndTime() {
            return jobVertexDetailsInfo.getEndTime();
        }

        @JsonIgnore
        public long getDuration() {
            return jobVertexDetailsInfo.getDuration();
        }

        @JsonIgnore
        public Map<ExecutionState, Integer> getTasksPerState() {
            return jobVertexDetailsInfo.getTasksPerState();
        }

        @JsonIgnore
        public IOMetricsInfo getJobVertexMetrics() {
            return jobVertexDetailsInfo.getJobVertexMetrics();
        }
    }
}

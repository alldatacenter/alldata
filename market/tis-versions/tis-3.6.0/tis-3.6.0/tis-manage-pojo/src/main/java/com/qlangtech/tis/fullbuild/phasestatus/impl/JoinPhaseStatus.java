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
import com.qlangtech.tis.fullbuild.phasestatus.IJoinTaskStatus;
import com.qlangtech.tis.fullbuild.phasestatus.IProcessDetailStatus;
import com.qlangtech.tis.fullbuild.phasestatus.JobLog;
import com.qlangtech.tis.fullbuild.phasestatus.impl.JoinPhaseStatus.JoinTaskStatus;
import org.apache.commons.lang.StringUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月17日
 */
public class JoinPhaseStatus extends BasicPhaseStatus<JoinTaskStatus> {

    @JSONField(serialize = false)
    public final Map<String, JoinTaskStatus> /*taskName*/
    taskStatus = Maps.newConcurrentMap();

    public JoinPhaseStatus(int taskid) {
        super(taskid);
    }

    @Override
    public boolean isShallOpen() {
        return shallOpenView(this.taskStatus.values());
    }

    /**
     * 设置当前任务全部结束,因为监听hive的日志经常不稳定，所以在特定步骤干脆直接设置成功就行了
     */
    public void setAllComplete() {
        taskStatus.values().forEach((t) -> {
            t.setComplete(true);
            t.setFaild(false);
            // JoinTaskStatus
            t.jobsLog().forEach((l) -> {
                l.setMapper(100);
                l.setReducer(100);
                l.setWaiting(false);
            });
        });
    }

    @Override
    protected FullbuildPhase getPhase() {
        return FullbuildPhase.JOIN;
    }

    @Override
    protected Collection<JoinTaskStatus> getChildStatusNode() {
        return this.taskStatus.values();
    }

    /**
     * 取得一个SQL对应的执行状态类
     *
     * @param taskName
     * @return
     */
    public JoinTaskStatus getTaskStatus(String taskName) {
        JoinTaskStatus sqlExecState = taskStatus.get(taskName);
        if (sqlExecState == null) {
            sqlExecState = new JoinTaskStatus(taskName);
            taskStatus.put(taskName, sqlExecState);
        }
        return sqlExecState;
    }

    /**
     * 对应每个Task的执行状态
     */
    public static class JoinTaskStatus extends AbstractChildProcessStatus implements IJoinTaskStatus {

        private final String joinTaskName;

        // 一个sql 会分成N个job执行
        @JSONField(serialize = false)
        public Map<Integer, JobLog> jobsStatus = new HashMap<>();

        public JoinTaskStatus(String taskname) {
            super();
            this.joinTaskName = taskname;
        }

        @Override
        public void setStart() {
            this.setWaiting(false);
        }

        @Override
        public String getAll() {
            return String.valueOf(jobsStatus.size() * 100);
        }

        @Override
        @JSONField(serialize = false)
        public Collection<JobLog> jobsLog() {
            return this.jobsStatus.values();
        }

        private long executed() {
            long result = 0;
            for (JobLog log : jobsStatus.values()) {
                result += log.getPercent();
            }
            return result;
        }

        @Override
        public String getProcessed() {
            return String.valueOf(executed());
        }

        @Override
        public int getPercent() {
            int size = jobsStatus.size();
            if (size > 0) {
                return (int) (executed() / size);
            }
            return 0;
        }

        @Override
        public String getName() {
            return this.joinTaskName;
        }

        @Override
        public JobLog getJoblog(Integer jobid) {
            JobLog jobLog = jobsStatus.get(jobid);
            if (jobLog == null) {
                throw new IllegalStateException("jobid:" + jobid + " relevant joblog can not be null,jobsStatus keys:" + jobsStatus.keySet());
            }
            return jobLog;
        }

        public void createJobStatus(Integer jobid) {
            jobsStatus.put(jobid, new JobLog());
            // 开始执行了
            this.setWaiting(false);
        }
    }

    @Override
    public IProcessDetailStatus<JoinTaskStatus> getProcessStatus() {
        return new IProcessDetailStatus<JoinTaskStatus>() {

            @Override
            public Collection<JoinTaskStatus> getDetails() {
                if (taskStatus.isEmpty()) {
                    JoinTaskStatus mock = new JoinTaskStatus(StringUtils.EMPTY);
                    mock.setWaiting(true);
                    return Collections.singleton(mock);
                }
                return taskStatus.values();
            }

            @Override
            public int getProcessPercent() {
                return 0;
            }

            @Override
            public void detailVisit(IChildProcessStatusVisitor visitor) {
                for (JoinTaskStatus status : taskStatus.values()) {
                    visitor.visit(status);
                }
            }
        };
    }
}

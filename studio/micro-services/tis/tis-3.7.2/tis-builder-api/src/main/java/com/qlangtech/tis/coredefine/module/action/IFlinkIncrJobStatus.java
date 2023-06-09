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

package com.qlangtech.tis.coredefine.module.action;

import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-04-15 13:46
 **/
public interface IFlinkIncrJobStatus<JobID> {
    String KEY_SAVEPOINT_DIR_PREFIX = "savepoint_";

    public enum State {
        // 之前运行着，突然flink-cluster被终止（可能因为机房断电），job与服务端失联
        DISAPPEAR,
        // 实例还未创建
        NONE
        // 实例已经停止
        , STOPED
        //
        , RUNNING;
    }

    /**
     * 当前任务的状态
     *
     * @return
     */
    public State getState();

    public JobID createNewJob(JobID jobID);

    public JobID getLaunchJobID();

    public void relaunch(JobID jobID);

    public void addSavePoint(String savepointDirectory, IFlinkIncrJobStatus.State state);

    public void discardSavepoint(String savepointDirectory);

    public void stop(String savepointDirectory) ;

    public void cancel();

    public Optional<FlinkSavepoint> containSavepoint(String path);

    public void setState(State state);

    /**
     * 历史checkpoint路径
     *
     * @return
     */
    public List<FlinkSavepoint> getSavepointPaths();

    public class FlinkSavepoint {
        private String path;
        private long createTimestamp;

        public FlinkSavepoint(String path, long createTimestamp) {
            this.path = path;
            this.createTimestamp = createTimestamp;
        }

        public String getPath() {
            return path;
        }

        public long getCreateTimestamp() {
            return createTimestamp;
        }
    }
}

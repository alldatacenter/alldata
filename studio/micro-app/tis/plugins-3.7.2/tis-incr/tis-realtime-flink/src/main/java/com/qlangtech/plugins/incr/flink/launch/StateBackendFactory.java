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

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.coredefine.module.action.IFlinkIncrJobStatus;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.order.center.IParamContext;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Optional;

/**
 * Flink 状态存储
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-03 16:32
 **/
@Public
public abstract class StateBackendFactory implements Describable<StateBackendFactory> {
    // public static final String OFF = "off";

    public abstract void setProps(StreamExecutionEnvironment env);

    /**
     * 缺的当前执行任务的状态
     *
     * @return
     */
    public IFlinkIncrJobStatus getIncrJobStatus(TargetResName collection) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " getIncrJobStatus is not supported");
    }

    /**
     * 支持Flink应用Savepoint功能
     */
    public interface ISavePointSupport {

        public boolean supportSavePoint();

        public String getSavePointRootPath();

        public default String createSavePointPath() {
            return getSavePointRootPath() + "/" + IFlinkIncrJobStatus.KEY_SAVEPOINT_DIR_PREFIX + IParamContext.getCurrentMillisecTimeStamp();
        }
    }

    /**
     * 支持从checkpint来恢复程序程序执行状态
     */
    public interface IRestoreFromCheckpointSupport {
        /**
         * 取得需要从某个需要恢复的checkpoint的路径（完整路径）
         *
         * @return
         */
        public Optional<String> getHistoryCheckpointPath();
    }

}

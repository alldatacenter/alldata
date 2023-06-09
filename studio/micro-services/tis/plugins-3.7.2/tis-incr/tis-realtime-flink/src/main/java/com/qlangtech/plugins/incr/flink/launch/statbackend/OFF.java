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

package com.qlangtech.plugins.incr.flink.launch.statbackend;

import com.qlangtech.plugins.incr.flink.launch.FlinkDescriptor;
import com.qlangtech.plugins.incr.flink.launch.StateBackendFactory;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.coredefine.module.action.IFlinkIncrJobStatus;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.extension.TISExtension;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-03 16:38
 **/
@Public
public class OFF extends StateBackendFactory {

    @Override
    public void setProps(StreamExecutionEnvironment env) {

    }

    @Override
    public IFlinkIncrJobStatus getIncrJobStatus(TargetResName collection) {
        return NonePersistBackendFlinkIncrJobStatus.getIncrStatus(collection);
    }

    @TISExtension()
    public static class DefaultDescriptor extends FlinkDescriptor<StateBackendFactory> {
        @Override
        public String getDisplayName() {
            return SWITCH_OFF;
        }
    }
}

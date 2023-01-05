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
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.StateBackendOptions;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackendFactory;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-03 17:01
 **/
@Public
public class MemoryState extends StateBackendFactory {

    @FormField(ordinal = 1, type = FormFieldType.ENUM, validate = {Validator.require})
    public Boolean latencyTrackEnable;

    @FormField(ordinal = 2, type = FormFieldType.INT_NUMBER,advance = true, validate = {Validator.integer, Validator.require})
    public Integer trackSampleInterval;

    @FormField(ordinal = 3, type = FormFieldType.INT_NUMBER,advance = true, validate = {Validator.integer, Validator.require})
    public Integer trackHistorySize;

    @Override
    public void setProps(StreamExecutionEnvironment env) {

        HashMapStateBackendFactory factory = new HashMapStateBackendFactory();

        Configuration config = new Configuration();

        config.setBoolean(StateBackendOptions.LATENCY_TRACK_ENABLED, this.latencyTrackEnable);
        config.setInteger(StateBackendOptions.LATENCY_TRACK_SAMPLE_INTERVAL, this.trackSampleInterval);
        config.setInteger(StateBackendOptions.LATENCY_TRACK_HISTORY_SIZE, this.trackHistorySize);

        env.setStateBackend(factory.createFromConfig(config, MemoryState.class.getClassLoader()));
    }

    @TISExtension()
    public static class DefaultDescriptor extends FlinkDescriptor<StateBackendFactory> {

        public DefaultDescriptor() {
            super();
            this.addFieldDescriptor("latencyTrackEnable", StateBackendOptions.LATENCY_TRACK_ENABLED);
            this.addFieldDescriptor("trackSampleInterval", StateBackendOptions.LATENCY_TRACK_SAMPLE_INTERVAL);
            this.addFieldDescriptor("trackHistorySize", StateBackendOptions.LATENCY_TRACK_HISTORY_SIZE);
        }


        @Override
        public String getDisplayName() {
            return "HashMapState";
        }


    }
}

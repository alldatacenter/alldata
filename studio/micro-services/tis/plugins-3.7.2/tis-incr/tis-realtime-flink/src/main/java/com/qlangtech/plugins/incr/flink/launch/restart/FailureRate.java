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

package com.qlangtech.plugins.incr.flink.launch.restart;

import com.qlangtech.plugins.incr.flink.launch.FlinkDescriptor;
import com.qlangtech.plugins.incr.flink.launch.FlinkJobRestartStrategy;
import com.qlangtech.plugins.incr.flink.launch.RestartStrategyFactory;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestartStrategyOptions;

import java.time.Duration;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-27 09:45
 **/
@Public
public class FailureRate extends RestartStrategyFactory {

    @FormField(ordinal = 1, type = FormFieldType.INT_NUMBER, validate = {Validator.integer, Validator.require})
    public Integer maxFailures;

    @FormField(ordinal = 2, type = FormFieldType.INT_NUMBER, validate = {Validator.integer, Validator.require})
    public Integer failureRateInterval;

    @FormField(ordinal = 3, type = FormFieldType.INT_NUMBER, validate = {Validator.integer, Validator.require})
    public Integer failureRateDelay;


    @Override
    protected Configuration getConfig() {
        Configuration conf = super.getConfig();
        conf.setInteger(
                RestartStrategyOptions
                        .RESTART_STRATEGY_FAILURE_RATE_MAX_FAILURES_PER_INTERVAL, maxFailures);
        conf.set(RestartStrategyOptions
                .RESTART_STRATEGY_FAILURE_RATE_FAILURE_RATE_INTERVAL, Duration.ofSeconds(failureRateInterval));
        conf.set(RestartStrategyOptions.RESTART_STRATEGY_FAILURE_RATE_DELAY, Duration.ofSeconds(failureRateDelay));
        return conf;
    }

    @TISExtension()
    public static class DefaultDescriptor extends FlinkDescriptor<RestartStrategyFactory> {
        public DefaultDescriptor() {
            super();
            this.addFieldDescriptor("maxFailures", RestartStrategyOptions.RESTART_STRATEGY_FAILURE_RATE_MAX_FAILURES_PER_INTERVAL);
            this.addFieldDescriptor("failureRateInterval", RestartStrategyOptions.RESTART_STRATEGY_FAILURE_RATE_FAILURE_RATE_INTERVAL);
            this.addFieldDescriptor("failureRateDelay", RestartStrategyOptions.RESTART_STRATEGY_FAILURE_RATE_DELAY);
        }

        @Override
        public String getDisplayName() {
            return FlinkJobRestartStrategy.FAILURE_RATE.val;
        }
    }
}

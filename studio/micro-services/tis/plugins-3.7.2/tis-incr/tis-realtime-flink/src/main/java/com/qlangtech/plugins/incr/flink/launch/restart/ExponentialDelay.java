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
 * @create: 2022-02-25 17:01
 **/
@Public
public class ExponentialDelay extends RestartStrategyFactory {

//    Duration initialBackoff =
//            configuration.get(
//                    RestartStrategyOptions
//                            .RESTART_STRATEGY_EXPONENTIAL_DELAY_INITIAL_BACKOFF);
//    Duration maxBackoff =
//            configuration.get(
//                    RestartStrategyOptions
//                            .RESTART_STRATEGY_EXPONENTIAL_DELAY_MAX_BACKOFF);
//    double backoffMultiplier =
//            configuration.get(
//                    RestartStrategyOptions
//                            .RESTART_STRATEGY_EXPONENTIAL_DELAY_BACKOFF_MULTIPLIER);
//    Duration resetBackoffThreshold =
//            configuration.get(
//                    RestartStrategyOptions
//                            .RESTART_STRATEGY_EXPONENTIAL_DELAY_RESET_BACKOFF_THRESHOLD);
//    double jitter =
//            configuration.get(
//                    RestartStrategyOptions
//                            .RESTART_STRATEGY_EXPONENTIAL_DELAY_JITTER_FACTOR);

    @FormField(ordinal = 1, type = FormFieldType.INT_NUMBER, validate = {Validator.integer, Validator.require})
    public Integer initialBackoff;
    @FormField(ordinal = 2, type = FormFieldType.INT_NUMBER, validate = {Validator.integer, Validator.require})
    public Integer maxBackoff;
    @FormField(ordinal = 3, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public double backoffMultiplier;
    @FormField(ordinal = 4, type = FormFieldType.INT_NUMBER, validate = {Validator.integer, Validator.require})
    public Integer resetBackoffThreshold;
    @FormField(ordinal = 5, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public double jitter;


    @Override
    protected Configuration getConfig() {
        Configuration conf = super.getConfig();
        conf.set(RestartStrategyOptions
                .RESTART_STRATEGY_EXPONENTIAL_DELAY_INITIAL_BACKOFF, Duration.ofSeconds(initialBackoff));
        conf.set(RestartStrategyOptions.RESTART_STRATEGY_EXPONENTIAL_DELAY_MAX_BACKOFF, Duration.ofSeconds(maxBackoff));
        conf.setDouble(RestartStrategyOptions.RESTART_STRATEGY_EXPONENTIAL_DELAY_BACKOFF_MULTIPLIER, backoffMultiplier);
        conf.set(RestartStrategyOptions
                .RESTART_STRATEGY_EXPONENTIAL_DELAY_RESET_BACKOFF_THRESHOLD, Duration.ofSeconds(resetBackoffThreshold));
        conf.setDouble(RestartStrategyOptions
                .RESTART_STRATEGY_EXPONENTIAL_DELAY_JITTER_FACTOR, jitter);
        return conf;
    }

    @TISExtension()
    public static class DefaultDescriptor extends FlinkDescriptor<RestartStrategyFactory> {

        public DefaultDescriptor() {
            super();
            this.addFieldDescriptor("initialBackoff", RestartStrategyOptions
                    .RESTART_STRATEGY_EXPONENTIAL_DELAY_INITIAL_BACKOFF);
            this.addFieldDescriptor("maxBackoff", RestartStrategyOptions.RESTART_STRATEGY_EXPONENTIAL_DELAY_MAX_BACKOFF);
            this.addFieldDescriptor("backoffMultiplier", RestartStrategyOptions.RESTART_STRATEGY_EXPONENTIAL_DELAY_BACKOFF_MULTIPLIER);
            this.addFieldDescriptor("resetBackoffThreshold", RestartStrategyOptions
                    .RESTART_STRATEGY_EXPONENTIAL_DELAY_RESET_BACKOFF_THRESHOLD);
            this.addFieldDescriptor("jitter", RestartStrategyOptions.RESTART_STRATEGY_EXPONENTIAL_DELAY_JITTER_FACTOR);

        }

        @Override
        public String getDisplayName() {
            return FlinkJobRestartStrategy.EXPONENTIAL_DELAY.val;
        }
    }
}

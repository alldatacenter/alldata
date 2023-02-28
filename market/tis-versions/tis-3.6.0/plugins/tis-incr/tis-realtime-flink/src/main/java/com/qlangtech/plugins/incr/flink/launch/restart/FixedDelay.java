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
 * @create: 2022-02-25 17:00
 **/
@Public
public class FixedDelay extends RestartStrategyFactory {

    @FormField(ordinal = 1, type = FormFieldType.INT_NUMBER, validate = {Validator.integer, Validator.require})
    public Integer attempts;

    @FormField(ordinal = 2, type = FormFieldType.INT_NUMBER, validate = {Validator.integer, Validator.require})
    public Integer delay;

    @Override
    protected Configuration getConfig() {
        Configuration cfg = super.getConfig();
        cfg.setInteger(RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_ATTEMPTS, attempts);
        cfg.set(RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_DELAY, Duration.ofSeconds(delay));
        return cfg;
    }

    @TISExtension()
    public static class DefaultDescriptor extends FlinkDescriptor<RestartStrategyFactory> {
        public DefaultDescriptor() {
            super();
            this.addFieldDescriptor("attempts", RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_ATTEMPTS);
            this.addFieldDescriptor("delay", RestartStrategyOptions.RESTART_STRATEGY_FIXED_DELAY_DELAY);
        }

        @Override
        public String getDisplayName() {
            return FlinkJobRestartStrategy.FIXED_DELAY.val;
        }
    }
}

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

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestartStrategyOptions;

import java.util.Objects;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-25 16:33
 **/
@Public
public abstract class RestartStrategyFactory implements Describable<RestartStrategyFactory> {

    public RestartStrategies.RestartStrategyConfiguration parseRestartStrategy() {
        Configuration cfg = getConfig();
        cfg.setString(RestartStrategyOptions.RESTART_STRATEGY
                , FlinkJobRestartStrategy.parse(this.getDescriptor().getDisplayName()).val);
        Optional<RestartStrategies.RestartStrategyConfiguration>
                restartStrategyCfg = RestartStrategies.fromConfiguration(cfg);
        if (!restartStrategyCfg.isPresent()) {
            throw new IllegalStateException("restartStrategyCfg must present");
        }
        return restartStrategyCfg.get();
    }

    protected Configuration getConfig() {
        return new Configuration();
    }

    @Override
    public final Descriptor<RestartStrategyFactory> getDescriptor() {
        return Objects.requireNonNull(TIS.get().getDescriptor(this.getClass())
                , "class:" + this.getClass().getName() + " relevant Descriptor can not be null");
    }

}

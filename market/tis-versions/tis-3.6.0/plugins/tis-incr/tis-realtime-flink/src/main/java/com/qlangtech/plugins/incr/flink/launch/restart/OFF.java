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

import com.qlangtech.plugins.incr.flink.launch.FlinkJobRestartStrategy;
import com.qlangtech.plugins.incr.flink.launch.RestartStrategyFactory;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-25 17:00
 **/
@Public
public class OFF extends RestartStrategyFactory {
    @TISExtension()
    public static class DefaultDescriptor extends Descriptor<RestartStrategyFactory> {
        @Override
        public String getDisplayName() {
            return FlinkJobRestartStrategy.OFF.val;
        }
    }
}

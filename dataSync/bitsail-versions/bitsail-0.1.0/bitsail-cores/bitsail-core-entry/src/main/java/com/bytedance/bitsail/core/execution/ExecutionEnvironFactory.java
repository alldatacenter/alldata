/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.core.execution;

import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.base.execution.Mode;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.core.command.CoreCommandArgs;
import com.bytedance.bitsail.flink.core.execution.FlinkExecutionEnviron;

import org.apache.commons.lang3.StringUtils;

/**
 * Created 2022/4/21
 */
public class ExecutionEnvironFactory {

  public static ExecutionEnviron getExecutionEnviron(CoreCommandArgs coreCommandArgs,
                                                     Mode mode,
                                                     BitSailConfiguration globalConfiguration) {
    String engineName = coreCommandArgs.getEngineName();
    if (StringUtils.equalsIgnoreCase(engineName, ExecutionEnvirons
        .FLINK.name())) {
      return new FlinkExecutionEnviron(globalConfiguration, mode);
    }
    throw new UnsupportedOperationException(String.format("Execution environ %s is not supported.", engineName));
  }
}

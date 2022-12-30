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

package com.bytedance.bitsail.test.connector.test;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.core.command.CoreCommandArgs;
import com.bytedance.bitsail.core.job.UnificationJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Created 2022/7/26
 */
public class EmbeddedFlinkCluster {
  private static final Logger LOG = LoggerFactory.getLogger(EmbeddedFlinkCluster.class);

  private static final long DEFAULT_JOB_ID = -1L;

  public static <T> void submitJob(BitSailConfiguration globalConfiguration) throws Exception {
    if (Objects.isNull(globalConfiguration)) {
      LOG.error("Submit failed, configuration is empty.");
      throw new IllegalStateException("Submit failed, configuration is empty");
    }
    overwriteConfiguration(globalConfiguration);
    LOG.info("Final Configuration: {}.\n", globalConfiguration.desensitizedBeautify());
    CoreCommandArgs coreCommandArgs = new CoreCommandArgs();
    coreCommandArgs.setEngineName("flink");
    UnificationJob<T> job = new UnificationJob<>(globalConfiguration, coreCommandArgs);
    job.start();
  }

  private static void overwriteConfiguration(BitSailConfiguration globalConfiguration) {
    globalConfiguration.set(CommonOptions.JOB_ID, DEFAULT_JOB_ID)
        .set(CommonOptions.SYNC_DDL, false)
        .set(CommonOptions.DRY_RUN, true)
        .set(CommonOptions.ENABLE_DYNAMIC_LOADER, false);
  }
}

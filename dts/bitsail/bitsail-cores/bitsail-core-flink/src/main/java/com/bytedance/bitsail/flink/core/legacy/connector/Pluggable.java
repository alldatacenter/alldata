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

package com.bytedance.bitsail.flink.core.legacy.connector;

import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.flink.core.constants.TypeSystem;

/**
 * @desc
 */
public interface Pluggable {

  /**
   * Initialize plugin in client.
   */
  void initPlugin() throws Exception;

  /**
   * Execute some terminate actions (<i>e.g.</i> analyze process result) when plugin successfully finished.
   *
   * @param result Process result returned by the job.
   */
  default void onSuccessComplete(ProcessResult<?> result) throws Exception {
  }

  /**
   * Execute some terminate actions when plugin failed.
   */
  default void onFailureComplete() throws Exception {
  }

  /**
   * Execute some terminate actions when job is terminated.
   */
  default void onDestroy() throws Exception {
  }

  /**
   * @return The type name of source.
   */
  String getType();

  /**
   * @return The type system of data in network transmission.
   */
  default TypeSystem getTypeSystem() {
    return TypeSystem.BitSail;
  }

  /**
   * @return Config options for adapter.
   */
  default BitSailConfiguration getAdapterConf() {
    return BitSailConfiguration.newDefault();
  }
}

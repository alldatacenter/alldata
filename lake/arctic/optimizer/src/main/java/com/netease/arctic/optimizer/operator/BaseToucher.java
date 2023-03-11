/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.netease.arctic.optimizer.operator;

import com.netease.arctic.ams.api.OptimizeManager;
import com.netease.arctic.ams.api.OptimizerStateReport;
import com.netease.arctic.ams.api.client.OptimizeManagerClientPools;
import com.netease.arctic.optimizer.OptimizerConfig;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;

/**
 * Report Optimizer State.
 */
public class BaseToucher implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(BaseToucher.class);
  private static final Map<String, String> EMPTY_STATE = Maps.newHashMap();

  private final OptimizerConfig config;

  public BaseToucher(OptimizerConfig config) {
    this.config = config;
  }

  /**
   * Only report optimizerId.
   * @return true if success
   */
  public boolean touch() {
    return touch(EMPTY_STATE);
  }

  /**
   * Report state, with optimizerId.
   * @param state -
   * @return true if success
   */
  public boolean touch(Map<String, String> state) {
    try {
      if (state == null) {
        state = EMPTY_STATE;
      }
      OptimizeManager.Iface client = OptimizeManagerClientPools.getClient(config.getAmsUrl());
      OptimizerStateReport report = new OptimizerStateReport();
      report.optimizerId = Long.parseLong(config.getOptimizerId());
      report.optimizerState = state;
      client.reportOptimizerState(report);
      LOG.info("touch {}", state);
      return true;
    } catch (Throwable t) {
      LOG.error("touch error", t);
      return false;
    }
  }
}

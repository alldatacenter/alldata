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

package com.netease.arctic.ams.api.client;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class PoolConfig extends GenericObjectPoolConfig {

  private int timeout = 0;

  private boolean failover = false;

  /**
   * get default connection socket timeout (default 0, means not timeout)
   *
   * @return
   */
  public int getTimeout() {
    return timeout;
  }

  /**
   * set default connection socket timeout
   *
   * @param timeout timeout millis
   */
  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  /**
   * get connect to next service if one service fail(default false)
   *
   * @return
   */
  public boolean isFailover() {
    return failover;
  }

  /**
   * set connect to next service if one service fail
   *
   * @param failover
   */
  public void setFailover(boolean failover) {
    this.failover = failover;
  }
}
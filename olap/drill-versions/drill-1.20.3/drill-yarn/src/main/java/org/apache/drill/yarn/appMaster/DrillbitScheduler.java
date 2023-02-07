/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.yarn.appMaster;

public class DrillbitScheduler extends AbstractDrillbitScheduler {
  private int requestTimeoutSecs;
  private int maxExtraNodes;


  public DrillbitScheduler(String name, TaskSpec taskSpec, int quantity,
                           int requestTimeoutSecs, int maxExtraNodes) {
    super("basic", name, quantity);
    this.taskSpec = taskSpec;
    this.requestTimeoutSecs = requestTimeoutSecs;
    this.maxExtraNodes = maxExtraNodes;
  }

  /**
   * Set the number of running tasks to the quantity given.
   * Limits the quantity to only a small margin above the number
   * of estimated free YARN nodes. This avoids a common users error
   * where someone requests 20 nodes on a 5-node cluster.
   */

  @Override
  public int resize(int level) {
    int limit = quantity + state.getController().getFreeNodeCount( ) +
        maxExtraNodes;
    return super.resize( Math.min( limit, level ) );
  }

  @Override
  public int getRequestTimeoutSec() {
    return requestTimeoutSecs;
  }
}

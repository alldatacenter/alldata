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
package org.apache.drill.exec;

import java.util.List;

import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.server.Drillbit;
import org.slf4j.Logger;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

/**
 * Base class for Drill system tests.
 * Starts one or more Drillbits, an embedded ZooKeeper cluster and provides a configured client for testing.
 */
public class DrillSystemTestBase extends TestWithZookeeper {
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(DrillSystemTestBase.class);

  private List<Drillbit> servers;

  public void startCluster(int numServers) {
    try {
      ImmutableList.Builder<Drillbit> servers = ImmutableList.builder();
      for (int i = 0; i < numServers; i++) {
        servers.add(Drillbit.start(zkHelper.getConfig()));
      }
      this.servers = servers.build();
    } catch (DrillbitStartupException e) {
      throw new RuntimeException(e);
    }
  }

  public void stopCluster() {
    if (servers != null) {
      for (Drillbit server : servers) {
        try {
          server.close();
        } catch (Exception e) {
          logger.warn("Error shutting down Drillbit", e);
        }
      }
    }
  }

  public Drillbit getABit(){
    return this.servers.iterator().next();
  }
}

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
package org.apache.drill.exec.work.foreman.rm;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.DrillbitContext;

/**
 * Abstract base class for a resource manager. Handles tasks common to all
 * resource managers: learning the resources available on this Drillbit.
 * In the current version, Drillbits must be symmetrical, so that knowing
 * the resources on one node is sufficient to know resources available on
 * all nodes.
 */

public abstract class AbstractResourceManager implements ResourceManager {

  protected final DrillbitContext context;
  private final long memoryPerNode;
  private final int cpusPerNode;

  public AbstractResourceManager(final DrillbitContext context) {
    this.context = context;
    DrillConfig config = context.getConfig();

    // Normally we use the actual direct memory configured on the JVM command
    // line. However, if the config param is set, we use that instead (if it is
    // lower than actual memory). Primarily for testing.

    long memLimit = DrillConfig.getMaxDirectMemory();
    long configMemoryPerNode = config.getBytes(ExecConstants.MAX_MEMORY_PER_NODE);
    if (configMemoryPerNode > 0) {
      memLimit = Math.min(memLimit, configMemoryPerNode);
    }
    memoryPerNode = memLimit;

    // Do the same for CPUs.

    int cpuLimit = Runtime.getRuntime().availableProcessors();
    int configCpusPerNode = config.getInt(ExecConstants.MAX_CPUS_PER_NODE);
    if (configCpusPerNode > 0) {
      cpuLimit = Math.min(cpuLimit, configCpusPerNode);
    }
    cpusPerNode = cpuLimit;
  }

  @Override
  public long memoryPerNode() { return memoryPerNode; }

  @Override
  public int cpusPerNode() { return cpusPerNode; }
}

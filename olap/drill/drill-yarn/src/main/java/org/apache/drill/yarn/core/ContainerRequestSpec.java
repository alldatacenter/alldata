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
package org.apache.drill.yarn.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.client.api.AMRMClient.ContainerRequest;
import org.apache.hadoop.yarn.util.Records;

/**
 * Describes a container request in terms of priority, memory, cores and
 * placement preference. This is a simplified version of the YARN
 * ContainerRequest structure. This structure is easier to use within the app,
 * then is translated to the YARN structure when needed.
 */

public class ContainerRequestSpec {
  static final Log LOG = LogFactory.getLog(ContainerRequestSpec.class);

  /**
   * Application-specific priority. Drill-on-Yarn uses the priority to associate
   * YARN requests with a {@link Scheduler}. When the resource allocation
   * arrives, we use the priority to trace back to the scheduler that requested
   * it, and from there to the task to be run in the allocation.
   * <p>
   * For this reason, the priority is set by the Drill-on-YARN application; it
   * is not a user-adjustable value.
   */

  public int priority = 0;

  /**
   * Memory, in MB, required by the container.
   */

  public int memoryMb;

  /**
   * Number of "virtual cores" required by the task. YARN allocates whole CPU
   * cores and does not support fractional allocations.
   */

  public int vCores = 1;

  /**
   * Number of virtual disks (channels, spindles) to request. Not supported in
   * Apache YARN, is supported in selected distributions.
   */

  public double disks;

  /**
   * Node label expression to apply to this request.
   */

  public String nodeLabelExpr;

  public List<String> racks = new ArrayList<>();
  public List<String> hosts = new ArrayList<>();

  /**
   * Create a YARN ContainerRequest object from the information in this object.
   *
   * @return
   */
  public ContainerRequest makeRequest() {
    assert memoryMb != 0;

    Priority priorityRec = Records.newRecord(Priority.class);
    priorityRec.setPriority(priority);

    Resource capability = Records.newRecord(Resource.class);
    capability.setMemory(memoryMb);
    capability.setVirtualCores(vCores);
    DoYUtil.callSetDiskIfExists(capability, disks);

    boolean relaxLocality = true;
    String nodeArr[] = null;
    if (!hosts.isEmpty()) {
      nodeArr = new String[hosts.size()];
      hosts.toArray(nodeArr);
      relaxLocality = false;
    }
    String rackArr[] = null;
    if (!racks.isEmpty()) {
      nodeArr = new String[racks.size()];
      racks.toArray(rackArr);
      relaxLocality = false;
    }
    String nodeExpr = null;
    if (!DoYUtil.isBlank(nodeLabelExpr)) {
      nodeExpr = nodeLabelExpr;
      LOG.info("Requesting a container using node expression: " + nodeExpr);
    }

    // YARN is fragile. To (potentially) pass a node expression, we must use the
    // 5-argument constructor. The fourth argument (relax locality) MUST be set
    // to true if we omit the rack and node specs. (Else we get a runtime
    // error.

    return new ContainerRequest(capability, nodeArr, rackArr, priorityRec,
        relaxLocality, nodeExpr);
  }
}

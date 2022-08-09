/**
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

package org.apache.ambari.msi;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Map;
import java.util.Set;

/**
 * A task resource provider for a MSI defined cluster.
 */
public class TaskProvider extends AbstractResourceProvider {

  // Tasks properties
  protected static final String TASK_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Tasks", "cluster_name");
  protected static final String TASK_REQUEST_ID_PROPERTY_ID   = PropertyHelper.getPropertyId("Tasks", "request_id");
  protected static final String TASK_ID_PROPERTY_ID           = PropertyHelper.getPropertyId("Tasks", "id");
  protected static final String TASK_STATUS_PROPERTY_ID       = PropertyHelper.getPropertyId("Tasks", "status");
  protected static final String TASK_EXIT_CODE_PROPERTY_ID    = PropertyHelper.getPropertyId("Tasks", "exit_code");
  protected static final String TASK_STDERR_PROPERTY_ID       = PropertyHelper.getPropertyId("Tasks", "stderr");
  protected static final String TASK_STOUT_PROPERTY_ID        = PropertyHelper.getPropertyId("Tasks", "stdout");


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a resource provider based on the given cluster definition.
   *
   * @param clusterDefinition  the cluster definition
   */
  public TaskProvider(ClusterDefinition clusterDefinition) {
    super(Resource.Type.Task, clusterDefinition);
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<Resource> getResources() {
    return getClusterDefinition().getTaskResources();
  }

  @Override
  public void updateProperties(Resource resource, Request request, Predicate predicate) {

    Integer taskId = (Integer) resource.getPropertyValue(TASK_ID_PROPERTY_ID);

    StateProvider.Process process = getClusterDefinition().getProcess(taskId);

    if (process != null) {
      resource.setProperty(TASK_STATUS_PROPERTY_ID, process.isRunning() ? "IN_PROGRESS" : "COMPLETED");

      Set<String> propertyIds = getRequestPropertyIds(request, predicate);

      if (contains(propertyIds, TASK_EXIT_CODE_PROPERTY_ID)) {
        resource.setProperty(TASK_EXIT_CODE_PROPERTY_ID, process.getExitCode());
      }
      if (contains(propertyIds, TASK_STDERR_PROPERTY_ID)) {
        resource.setProperty(TASK_STDERR_PROPERTY_ID, process.getError());
      }
      if (contains(propertyIds, TASK_STOUT_PROPERTY_ID)) {
        resource.setProperty(TASK_STOUT_PROPERTY_ID, process.getOutput());
      }
    }
  }

  @Override
  public int updateProperties(Resource resource, Map<String, Object> properties) {
    //Do nothing
    return -1;
  }
}

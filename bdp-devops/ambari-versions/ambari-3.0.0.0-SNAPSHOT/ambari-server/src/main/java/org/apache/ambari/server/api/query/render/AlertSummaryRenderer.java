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

package org.apache.ambari.server.api.query.render;

import java.util.Set;

import org.apache.ambari.server.api.query.QueryInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultPostProcessor;
import org.apache.ambari.server.api.services.ResultPostProcessorImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.internal.AlertResourceProvider;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.MaintenanceState;

/**
 * The {@link AlertSummaryRenderer} is used to format the results of queries to
 * the alerts endpoint. Each item returned from the query represents an
 * individual current alert which is then aggregated into a summary structure
 * based on the alert state.
 * <p/>
 * The finalized structure is:
 *
 * <pre>
 * {
 *   "href" : "http://localhost:8080/api/v1/clusters/c1/alerts?format=summary",
 *   "alerts_summary" : {
 *     "CRITICAL" : {
 *       "count" : 3,
 *       "maintenance_count" : 1,
 *       "original_timestamp" : 1415372828182
 *     },
 *     "OK" : {
 *       "count" : 37,
 *       "maintenance_count" : 0,
 *       "original_timestamp" : 1415375364937
 *     },
 *     "UNKNOWN" : {
 *       "count" : 1,
 *       "maintenance_count" : 0,
 *       "original_timestamp" : 1415372632261
 *     },
 *     "WARN" : {
 *       "count" : 0,
 *       "maintenance_count" : 0,
 *       "original_timestamp" : 0
 *     }
 *   }
 * }
 * </pre>
 * <p/>
 * The nature of a {@link Renderer} is that it manipulates the dataset returned
 * by a query. In the case of alert data, the query could potentially return
 * thousands of results if there are thousands of nodes in the cluster. This
 * could present a performance issue that can only be addressed by altering the
 * incoming query and modifying it to instruct the backend to return a JPA SUM
 * instead of a collection of entities.
 */
public class AlertSummaryRenderer extends BaseRenderer implements Renderer {

  /**
   * {@inheritDoc}
   */
  @Override
  public TreeNode<Set<String>> finalizeProperties(
      TreeNode<QueryInfo> queryTree, boolean isCollection) {

    QueryInfo queryInfo = queryTree.getObject();
    TreeNode<Set<String>> resultTree = new TreeNodeImpl<>(
      null, queryInfo.getProperties(), queryTree.getName());

    copyPropertiesToResult(queryTree, resultTree);

    boolean addKeysToEmptyResource = true;
    if (!isCollection && isRequestWithNoProperties(queryTree)) {
      addSubResources(queryTree, resultTree);
      addKeysToEmptyResource = false;
    }

    ensureRequiredProperties(resultTree, addKeysToEmptyResource);

    // ensure that state and original_timestamp are on the request since these
    // are required by the finalization process of this renderer
    Set<String> properties = resultTree.getObject();
    addRequiredAlertProperties(properties);

    return resultTree;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ResultPostProcessor getResultPostProcessor(Request request) {
    // simply return the native rendering
    return new ResultPostProcessorImpl(request);
  }

  /**
   * {@inheritDoc}
   * <p/>
   * This will iterate over all of the nodes in the result tree and combine
   * their {@link AlertResourceProvider#ALERT_STATE} into a single summary
   * structure.
   */
  @Override
  public Result finalizeResult(Result queryResult) {
    TreeNode<Resource> resultTree = queryResult.getResultTree();
    AlertStateSummary alertSummary = new AlertStateSummary();

    // iterate over all returned flattened alerts and build the summary info
    for (TreeNode<Resource> node : resultTree.getChildren()) {
      Resource resource = node.getObject();
      AlertState state = (AlertState) resource.getPropertyValue(AlertResourceProvider.ALERT_STATE);
      Long originalTimestampObject = (Long) resource.getPropertyValue(AlertResourceProvider.ALERT_ORIGINAL_TIMESTAMP);
      MaintenanceState maintenanceState = (MaintenanceState) resource.getPropertyValue(AlertResourceProvider.ALERT_MAINTENANCE_STATE);

      // NPE sanity
      if (null == state) {
        state = AlertState.UNKNOWN;
      }

      // NPE sanity
      long originalTimestamp = 0;
      if (null != originalTimestampObject) {
        originalTimestamp = originalTimestampObject.longValue();
      }

      // NPE sanity
      boolean isMaintenanceModeEnabled = false;
      if (null != maintenanceState && maintenanceState != MaintenanceState.OFF) {
        isMaintenanceModeEnabled = true;
      }

      final AlertStateValues alertStateValues;
      switch (state) {
        case CRITICAL: {
          alertStateValues = alertSummary.Critical;
          break;
        }
        case OK: {
          alertStateValues = alertSummary.Ok;
          break;
        }
        case WARNING: {
          alertStateValues = alertSummary.Warning;
          break;
        }
        default:
        case UNKNOWN: {
          alertStateValues = alertSummary.Unknown;
          break;
        }
      }

      if (isMaintenanceModeEnabled) {
        alertStateValues.MaintenanceCount++;
      } else {
        alertStateValues.Count++;
      }

      if (originalTimestamp > alertStateValues.Timestamp) {
        alertStateValues.Timestamp = originalTimestamp;
      }
    }

    Result summary = new ResultImpl(true);
    Resource resource = new ResourceImpl(Resource.Type.Alert);
    TreeNode<Resource> summaryTree = summary.getResultTree();
    summaryTree.addChild(resource, "alerts_summary");
    resource.setProperty("alerts_summary", alertSummary);

    return summary;
  }

  /**
   * Adds properties to the backend request that are required by this renderer.
   * This method currently adds {@link AlertResourceProvider#ALERT_STATE} and
   * {@link AlertResourceProvider#ALERT_ORIGINAL_TIMESTAMP}.
   *
   * @param properties
   *          the properties collection to add to.
   */
  protected void addRequiredAlertProperties(Set<String> properties) {
    properties.add(AlertResourceProvider.ALERT_STATE);
    properties.add(AlertResourceProvider.ALERT_ORIGINAL_TIMESTAMP);
    properties.add(AlertResourceProvider.ALERT_MAINTENANCE_STATE);
  }
}

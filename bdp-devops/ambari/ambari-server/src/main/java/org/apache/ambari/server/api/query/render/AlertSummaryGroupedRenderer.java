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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.internal.AlertResourceProvider;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.MaintenanceState;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * The {@link AlertSummaryGroupedRenderer} is used to format the results of
 * queries to the alerts endpoint. Each alert instance returned from the backend
 * is grouped by its alert definition and its state is then aggregated into the
 * summary information for that definition.
 * <p/>
 * The finalized structure is:
 *
 * <pre>
 * {
 *   "alerts_summary_grouped" : [
 *     {
 *       "definition_id" : 1,
 *       "definition_name" : "datanode_process",
 *       "summary" : {
 *         "CRITICAL": {
 *           "count": 1,
 *           "maintenance_count" : 1,
 *           "original_timestamp": 1415372992337,
 *           "latest_text" : "TCP Connection Failure"
 *         },
 *         "OK": {
 *           "count": 1,
 *           "maintenance_count" : 0,
 *           "original_timestamp": 1415372992337,
 *           "latest_text" : "TCP OK"
 *         },
 *         "UNKNOWN": {
 *           "count": 0,
 *           "maintenance_count" : 0,
 *           "original_timestamp": 0
 *         },
 *        "WARN": {
 *          "count": 0,
 *          "maintenance_count" : 0,
 *          "original_timestamp": 0
 *         }
 *       }
 *     },
 *     {
 *       "definition_id" : 2,
 *       "definition_name" : "namenode_process",
 *       "summary" : {
 *         "CRITICAL": {
 *           "count": 1,
 *           "maintenance_count" : 0,
 *           "original_timestamp": 1415372992337
 *         },
 *         "OK": {
 *           "count": 1,
 *           "maintenance_count" : 0,
 *           "original_timestamp": 1415372992337
 *         },
 *         "UNKNOWN": {
 *           "count": 0,
 *           "maintenance_count" : 0,
 *           "original_timestamp": 0
 *         },
 *        "WARN": {
 *          "count": 0,
 *          "maintenance_count" : 0,
 *          "original_timestamp": 0
 *         }
 *       }
 *     }
 *   ]
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
public class AlertSummaryGroupedRenderer extends AlertSummaryRenderer {

  private static final String ALERTS_SUMMARY_GROUP = "alerts_summary_grouped";

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
    Map<String, AlertDefinitionSummary> summaries = new HashMap<>();

    // iterate over all returned flattened alerts and build the summary info
    for (TreeNode<Resource> node : resultTree.getChildren()) {
      Resource resource = node.getObject();

      Long definitionId = (Long) resource.getPropertyValue(AlertResourceProvider.ALERT_DEFINITION_ID);
      String definitionName = (String) resource.getPropertyValue(AlertResourceProvider.ALERT_DEFINITION_NAME);
      AlertState state = (AlertState) resource.getPropertyValue(AlertResourceProvider.ALERT_STATE);
      Long originalTimestampObject = (Long) resource.getPropertyValue(AlertResourceProvider.ALERT_ORIGINAL_TIMESTAMP);
      MaintenanceState maintenanceState = (MaintenanceState) resource.getPropertyValue(AlertResourceProvider.ALERT_MAINTENANCE_STATE);
      String alertText = (String) resource.getPropertyValue(AlertResourceProvider.ALERT_TEXT);

      updateSummary(summaries, definitionId, definitionName, state, originalTimestampObject, maintenanceState, alertText);
    }

    Set<Entry<String, AlertDefinitionSummary>> entrySet = summaries.entrySet();
    List<AlertDefinitionSummary> groupedResources = new ArrayList<>(
      entrySet.size());

    // iterate over all summary groups, adding them to the final list
    for (Entry<String, AlertDefinitionSummary> entry : entrySet) {
      groupedResources.add(entry.getValue());
    }

    Result groupedSummary = new ResultImpl(true);
    TreeNode<Resource> summaryTree = groupedSummary.getResultTree();

    Resource resource = new ResourceImpl(Resource.Type.Alert);
    summaryTree.addChild(resource, ALERTS_SUMMARY_GROUP);

    resource.setProperty(ALERTS_SUMMARY_GROUP, groupedResources);
    return groupedSummary;
  }

  public static void updateSummary(Map<String, AlertDefinitionSummary> summaries, Long definitionId, String definitionName,
                            AlertState state, Long originalTimestampObject, MaintenanceState maintenanceState,
                            String alertText) {
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

    // create the group summary info if it doesn't exist yet
    AlertDefinitionSummary groupSummaryInfo = summaries.get(definitionName);
    if (null == groupSummaryInfo) {
      groupSummaryInfo = new AlertDefinitionSummary();
      groupSummaryInfo.Id = definitionId;
      groupSummaryInfo.Name = definitionName;

      summaries.put(definitionName, groupSummaryInfo);
    }

    // set and increment the correct values based on state
    final AlertStateValues alertStateValues;
    switch (state) {
      case CRITICAL: {
        alertStateValues = groupSummaryInfo.State.Critical;
        break;
      }
      case OK: {
        alertStateValues = groupSummaryInfo.State.Ok;
        break;
      }
      case WARNING: {
        alertStateValues = groupSummaryInfo.State.Warning;
        break;
      }
      default:
      case UNKNOWN: {
        alertStateValues = groupSummaryInfo.State.Unknown;
        break;
      }
    }

    // update the maintenance count if in MM is enabled, otherwise the
    // regular count
    if (isMaintenanceModeEnabled) {
      alertStateValues.MaintenanceCount++;
    } else {
      alertStateValues.Count++;
    }

    // check to see if this alerts time is sooner; if so, keep track of it
    // and of its text
    if (originalTimestamp > alertStateValues.Timestamp) {
      alertStateValues.Timestamp = originalTimestamp;
      alertStateValues.AlertText = alertText;
    }
  }

  public static Map<String, AlertDefinitionSummary> generateEmptySummary(Long definitionId, String definitionName) {
    Map<String, AlertDefinitionSummary> summaries = new HashMap<>();

    AlertDefinitionSummary groupSummaryInfo = new AlertDefinitionSummary();
    groupSummaryInfo.Id = definitionId;
    groupSummaryInfo.Name = definitionName;

    summaries.put(definitionName, groupSummaryInfo);

    return summaries;
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Additionally adds {@link AlertResourceProvider#ALERT_ID} and
   * {@link AlertResourceProvider#ALERT_DEFINITION_NAME}.
   */
  @Override
  protected void addRequiredAlertProperties(Set<String> properties) {
    super.addRequiredAlertProperties(properties);

    properties.add(AlertResourceProvider.ALERT_ID);
    properties.add(AlertResourceProvider.ALERT_DEFINITION_NAME);
    properties.add(AlertResourceProvider.ALERT_MAINTENANCE_STATE);
    properties.add(AlertResourceProvider.ALERT_TEXT);
  }

  /**
   * The {@link AlertDefinitionSummary} is a simple data structure for keeping
   * track of each alert definition's summary information as the result set is
   * being iterated over.
   */
  public final static class AlertDefinitionSummary {
    @JsonProperty(value = "definition_id")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "definition_id")
    public long Id;

    @JsonProperty(value = "definition_name")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "definition_name")
    public String Name;

    @JsonProperty(value = "summary")
    @com.fasterxml.jackson.annotation.JsonProperty(value = "summary")
    public final AlertStateSummary State = new AlertStateSummary();

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      AlertDefinitionSummary that = (AlertDefinitionSummary) o;

      if (Id != that.Id) return false;
      if (Name != null ? !Name.equals(that.Name) : that.Name != null) return false;
      return State != null ? State.equals(that.State) : that.State == null;
    }

    @Override
    public int hashCode() {
      int result = (int) (Id ^ (Id >>> 32));
      result = 31 * result + (Name != null ? Name.hashCode() : 0);
      result = 31 * result + (State != null ? State.hashCode() : 0);
      return result;
    }
  }
}

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
package org.apache.ambari.server.alerts;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestStatus;
import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.api.query.QueryImpl;
import org.apache.ambari.server.api.query.render.MinimalRenderer;
import org.apache.ambari.server.api.resources.ClusterResourceDefinition;
import org.apache.ambari.server.api.services.BaseRequest;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.security.authorization.internal.InternalAuthenticationToken;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.ParameterizedSource.AlertParameter;
import org.apache.ambari.server.state.alert.ServerSource;
import org.apache.ambari.server.state.services.AmbariServerAlertService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Inject;

/**
 * The {@link AmbariPerformanceRunnable} is used by the
 * {@link AmbariServerAlertService} to ensure that certain areas of Ambari are
 * responsive. It performs the following checks:
 * <ul>
 * <li>A GET request against the cluster endpoint.</li>
 * <li>A query against {@link HostRoleCommandDAO} to get a summary of request
 * statuses</li>
 * <ul>
 */
public class AmbariPerformanceRunnable extends AlertRunnable {

  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(AmbariPerformanceRunnable.class);

  /**
   * <pre>
   * Performance Overview:
   *   Database Access (Request By Status): 330ms (OK)
   *   REST API (Cluster Request): 5,456ms (WARNING)
   * </pre>
   */
  private static final String PERFORMANCE_OVERVIEW_TEMPLATE = "Performance Overview:"
      + System.lineSeparator() + "{0}";

  /**
   * Example: {@code Database Access (Request By Status): 330ms (OK)}
   */
  private static final String PERFORMANCE_AREA_TEMPLATE = "  {0}: {1}ms ({2})";

  /**
   * Example:
   * {@code Unable to execute performance alert area REQUEST_BY_STATUS (UNKNOWN)}
   */
  private static final String PERFORMANCE_AREA_FAILURE_TEMPLATE = "  Unable to execute performance alert area {0}: ({1})";

  /**
   * Used for converting {@link AlertDefinitionEntity} into
   * {@link AlertDefinition} instances.
   */
  @Inject
  private AlertDefinitionFactory m_definitionFactory;

  /**
   * The {@link PerformanceArea} enumeration represents logical areas of
   * functionality to test for performance.
   */
   enum PerformanceArea {

    /**
     * Query for requests by {@link RequestStatus#IN_PROGRESS}.
     */
    REQUEST_BY_STATUS("Database Access (Request By Status)",
        "request.by.status.warning.threshold", 3000, "request.by.status.critical.threshold", 5000) {
      /**
       * {@inheritDoc}
       */
      @Override
      void execute(AmbariPerformanceRunnable runnable, Cluster cluster) throws Exception {
        runnable.m_actionManager.getRequestsByStatus(RequestStatus.IN_PROGRESS,
            BaseRequest.DEFAULT_PAGE_SIZE, false);
      }
    },

    /**
     * Query for requests by {@link RequestStatus#IN_PROGRESS}.
     */
    HRC_SUMMARY_STATUS("Database Access (Task Status Aggregation)",
        "task.status.aggregation.warning.threshold", 3000,
        "task.status.aggregation.critical.threshold", 5000) {
      /**
       * {@inheritDoc}
       */
      @Override
      void execute(AmbariPerformanceRunnable runnable, Cluster cluster) throws Exception {
        List<Long> requestIds = runnable.m_requestDAO.findAllRequestIds(
            BaseRequest.DEFAULT_PAGE_SIZE, false);

        for (long requestId : requestIds) {
          runnable.m_hostRoleCommandDAO.findAggregateCounts(requestId);
        }
      }
    },

    /**
     * Query through the REST API framework for a cluster.
     */
    REST_API_GET_CLUSTER("REST API (Cluster)",
        "rest.api.cluster.warning.threshold",
        5000, "rest.api.cluster.critical.threshold", 7000) {
      /**
       * {@inheritDoc}
       */
      @Override
      void execute(AmbariPerformanceRunnable runnable, Cluster cluster) throws Exception {
        // Set authenticated user so that authorization checks will pass
        InternalAuthenticationToken authenticationToken = new InternalAuthenticationToken("admin");
        authenticationToken.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        // create the request
        Map<Resource.Type, String> mapIds = new HashMap<>();
        mapIds.put(Resource.Type.Cluster, cluster.getClusterName());

        ClusterController clusterController = ClusterControllerHelper.getClusterController();
        Query query = new QueryImpl(mapIds, new ClusterResourceDefinition(), clusterController);
        query.setRenderer(new MinimalRenderer());
        query.addProperty("Clusters/desired_configs", null);
        query.addProperty("Clusters/credential_store_properties", null);
        query.addProperty("Clusters/desired_service_config_versions", null);
        query.addProperty("Clusters/health_report", null);
        query.addProperty("Clusters/total_hosts", null);
        query.addProperty("alerts_summary", null);
        query.addProperty("alerts_summary_hosts", null);
        query.execute();
      }
    };

    /**
     * The label for the performance area.
     */
    private final String m_label;

    /**
     * The name of the parameter on the alert definition which represents the
     * {@link AlertState#WARNING} threshold value.
     */
    private final String m_warningParameter;

    /**
     * A default {@link AlertState#WARNING} threshold value of the definition
     * doesn't have {@link #m_warningParameter} defined.
     */
    private final int m_defaultWarningThreshold;

    /**
     * The name of the parameter on the alert definition which represents the
     * {@link AlertState#CRITICAL} threshold value.
     */
    private final String m_criticalParameter;

    /**
     * A default {@link AlertState#WARNING} threshold value of the definition
     * doesn't have {@link #m_criticalParameter} defined.
     */
    private final int m_defaultCriticalThreshold;

    /**
     * Constructor.
     *
     * @param label
     *          the display label for this performance area (not {@code null}).
     * @param warningParameter
     *          the definition parameter name for the warning threshold (not
     *          {@code null})
     * @param defaultWarningThreshold
     *          the default value to use if the definition does not have a
     *          warning threshold paramter.
     * @param criticalParameter
     *          the definition parameter name for the critical threshold (not
     *          {@code null})
     * @param defaultCriticalThreshold
     *          the default value to use if the definition does not have a
     *          critical threshold paramter.
     */
    PerformanceArea(String label, String warningParameter, int defaultWarningThreshold,
                    String criticalParameter, int defaultCriticalThreshold) {
      m_label = label;
      m_warningParameter = warningParameter;
      m_defaultWarningThreshold = defaultWarningThreshold;
      m_criticalParameter = criticalParameter;
      m_defaultCriticalThreshold = defaultCriticalThreshold;
    }

    /**
     * Runs the {@link PerformanceArea}.
     *
     * @param runnable
     *          a reference to the parent {@link AlertRunnable} which has
     *          injected members for use.
     * @return a result of running the performance area (never {@code null}).
     */
    abstract void execute(AmbariPerformanceRunnable runnable, Cluster cluster) throws Exception;
  }

  /**
   * Used for getting the most recent requests.
   */
  @Inject
  private RequestDAO m_requestDAO;

  /**
   * Used for executing queries which are known to potentially take a long time.
   */
  @Inject
  private HostRoleCommandDAO m_hostRoleCommandDAO;

  /**
   * Used for querying for requests by status.
   */
  @Inject
  private ActionManager m_actionManager;

  /**
   * Constructor.
   *
   * @param definitionName
   */
  public AmbariPerformanceRunnable(String definitionName) {
    super(definitionName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  List<Alert> execute(Cluster cluster, AlertDefinitionEntity entity) throws AmbariException {
    // coerce the entity into a business object so that the list of parameters
    // can be extracted and used for threshold calculation
    AlertDefinition definition = m_definitionFactory.coerce(entity);
    ServerSource serverSource = (ServerSource) definition.getSource();
    List<AlertParameter> parameters = serverSource.getParameters();
    List<String> results = new ArrayList<>();

    // start out assuming OK
    AlertState alertState = AlertState.OK;

    // run every performance area
    for (PerformanceArea performanceArea : PerformanceArea.values()) {
      // execute the performance area, creating an UNKNOWN state on exceptions
      PerformanceResult performanceResult;
      try {
        long startTime = System.currentTimeMillis();
        performanceArea.execute(this, cluster);
        long totalTime = System.currentTimeMillis() - startTime;

        performanceResult = calculatePerformanceResult(performanceArea, totalTime, parameters);

      } catch (Exception exception) {
        String result = MessageFormat.format(PERFORMANCE_AREA_FAILURE_TEMPLATE, performanceArea,
            AlertState.UNKNOWN);

        LOG.error(result, exception);
        performanceResult = new PerformanceResult(result, AlertState.UNKNOWN);
      }

      String result = performanceResult.getResult();
      AlertState resultAlertState = performanceResult.getAlertState();

      // keep track of the string result for formatting later
      results.add(result);

      // keep track of the overall state of "this" alert
      switch (resultAlertState) {
        case CRITICAL:
          alertState = AlertState.CRITICAL;
          break;
        case OK:
          break;
        case SKIPPED:
          break;
        case UNKNOWN:
          if (alertState == AlertState.OK) {
            alertState = AlertState.UNKNOWN;
          }
          break;
        case WARNING:
          if (alertState != AlertState.CRITICAL) {
            alertState = AlertState.WARNING;
          }
          break;
        default:
          break;
      }
    }

    // create a text overview of all of the runs
    String allResults = StringUtils.join(results, System.lineSeparator());
    String overview = MessageFormat.format(PERFORMANCE_OVERVIEW_TEMPLATE, allResults);

    // build the alert to return
    Alert alert = new Alert(entity.getDefinitionName(), null, entity.getServiceName(),
        entity.getComponentName(), null, alertState);

    alert.setLabel(entity.getLabel());
    alert.setText(overview);
    alert.setTimestamp(System.currentTimeMillis());
    alert.setClusterId(cluster.getClusterId());

    return Collections.singletonList(alert);
  }

  /**
   * Calculates the state based on the threshold values for a
   * {@link PerformanceArea} and an actual run time.
   *
   * @param area
   *          the area to calculate the result for (not {@code null}).
   * @param time
   *          the time taken, in milliseconds, to run the test.
   * @param parameters
   *          a list of parameters from the alert definition which contain the
   *          threshold values.
   * @return a result of running the performance area (never {@code null}).
   */
  PerformanceResult calculatePerformanceResult(PerformanceArea area, long time,
      List<AlertParameter> parameters) {
    AlertState alertState = AlertState.OK;
    int warningThreshold = area.m_defaultWarningThreshold;
    int criticalThreshold = area.m_defaultCriticalThreshold;

    for (AlertParameter parameter : parameters) {
      Object value = parameter.getValue();

      if (StringUtils.equals(parameter.getName(), area.m_warningParameter)) {
        warningThreshold = alertHelper.getThresholdValue(value, warningThreshold);
      }

      if (StringUtils.equals(parameter.getName(), area.m_criticalParameter)) {
        criticalThreshold = alertHelper.getThresholdValue(value, criticalThreshold);
      }
    }

    if (time >= warningThreshold && time < criticalThreshold) {
      alertState = AlertState.WARNING;
    }

    if (time >= criticalThreshold) {
      alertState = AlertState.CRITICAL;
    }

    String resultLabel = MessageFormat.format(PERFORMANCE_AREA_TEMPLATE, area.m_label, time,
        alertState);

    return new PerformanceResult(resultLabel, alertState);
  }

  /**
   * The {@link PerformanceResult} class is used to wrap the result of a
   * {@link PerformanceArea}.
   */
  private static final class PerformanceResult {
    private final String m_result;
    private final AlertState m_alertState;

    /**
     * Constructor.
     *
     * @param result
     *          the text of the result (not {@code null}).
     * @param alertState
     *          the result state (not {@code null}).
     */
    private PerformanceResult(String result, AlertState alertState) {
      m_result = result;
      m_alertState = alertState;
    }

    /**
     * Gets the fully-rendered result text, such as:
     * {@code Database Access (Request By Status): 330ms (OK)}
     *
     * @return the result
     */
    public String getResult() {
      return m_result;
    }

    /**
     * The state of the result as calculated by the threshold parameters.
     *
     * @return the state
     */
    public AlertState getAlertState() {
      return m_alertState;
    }
  }
}

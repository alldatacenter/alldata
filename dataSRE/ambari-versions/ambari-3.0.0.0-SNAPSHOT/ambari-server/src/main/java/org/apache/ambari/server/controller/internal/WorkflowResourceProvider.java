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

package org.apache.ambari.server.controller.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.jdbc.ConnectionFactory;
import org.apache.ambari.server.controller.jdbc.JobHistoryPostgresConnectionFactory;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * Resource provider for workflow resources.
 */
public class WorkflowResourceProvider extends
    AbstractJDBCResourceProvider<WorkflowResourceProvider.WorkflowFields> {
  private static final Logger LOG = LoggerFactory.getLogger(WorkflowResourceProvider.class);

  protected static final String WORKFLOW_CLUSTER_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Workflow", "cluster_name");
  protected static final String WORKFLOW_ID_PROPERTY_ID = PropertyHelper
      .getPropertyId("Workflow", "workflow_id");
  protected static final String WORKFLOW_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Workflow", "name");
  protected static final String WORKFLOW_USER_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Workflow", "user_name");
  protected static final String WORKFLOW_START_TIME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Workflow", "start_time");
  protected static final String WORKFLOW_LAST_UPDATE_TIME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Workflow", "last_update_time");
  protected static final String WORKFLOW_ELAPSED_TIME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Workflow", "elapsed_time");
  protected static final String WORKFLOW_INPUT_BYTES_PROPERTY_ID = PropertyHelper
      .getPropertyId("Workflow", "input_bytes");
  protected static final String WORKFLOW_OUTPUT_BYTES_PROPERTY_ID = PropertyHelper
      .getPropertyId("Workflow", "output_bytes");
  protected static final String WORKFLOW_NUM_JOBS_TOTAL_PROPERTY_ID = PropertyHelper
      .getPropertyId("Workflow", "num_jobs_total");
  protected static final String WORKFLOW_NUM_JOBS_COMPLETED_PROPERTY_ID = PropertyHelper
      .getPropertyId("Workflow", "num_jobs_completed");
  protected static final String WORKFLOW_PARENT_ID_PROPERTY_ID = PropertyHelper
      .getPropertyId("Workflow", "parent_id");
  protected static final String WORKFLOW_CONTEXT_PROPERTY_ID = PropertyHelper
      .getPropertyId("Workflow", "context");

  protected WorkflowFetcher workflowFetcher;

  /**
   * The key property ids for a Workflow resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Type.Cluster, WORKFLOW_CLUSTER_NAME_PROPERTY_ID)
      .put(Type.Workflow, WORKFLOW_ID_PROPERTY_ID)
      .build();

  /**
   * The property ids for a Workflow resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      WORKFLOW_CLUSTER_NAME_PROPERTY_ID,
      WORKFLOW_ID_PROPERTY_ID,
      WORKFLOW_NAME_PROPERTY_ID,
      WORKFLOW_USER_NAME_PROPERTY_ID,
      WORKFLOW_START_TIME_PROPERTY_ID,
      WORKFLOW_LAST_UPDATE_TIME_PROPERTY_ID,
      WORKFLOW_ELAPSED_TIME_PROPERTY_ID,
      WORKFLOW_INPUT_BYTES_PROPERTY_ID,
      WORKFLOW_OUTPUT_BYTES_PROPERTY_ID,
      WORKFLOW_NUM_JOBS_TOTAL_PROPERTY_ID,
      WORKFLOW_NUM_JOBS_COMPLETED_PROPERTY_ID,
      WORKFLOW_PARENT_ID_PROPERTY_ID,
      WORKFLOW_CONTEXT_PROPERTY_ID);

  /**
   * Create a new workflow resource provider.
   */
  protected WorkflowResourceProvider() {
    super(propertyIds, keyPropertyIds);
    this.workflowFetcher = new PostgresWorkflowFetcher(
        new JobHistoryPostgresConnectionFactory());
  }

  /**
   * Create a new workflow resource provider.
   * 
   * @param workflowFetcher
   *          workflow fetcher
   */
  protected WorkflowResourceProvider(WorkflowFetcher workflowFetcher) {
    super(propertyIds, keyPropertyIds);
    this.workflowFetcher = workflowFetcher;
  }

  @Override
  public RequestStatus createResources(Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resourceSet = new HashSet<>();
    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<Map<String,Object>> predicatePropertieSet = getPropertyMaps(predicate);
    for (Map<String,Object> predicateProperties : predicatePropertieSet) {
      String clusterName = (String) predicateProperties
          .get(WORKFLOW_CLUSTER_NAME_PROPERTY_ID);
      String workflowId = (String) predicateProperties
          .get(WORKFLOW_ID_PROPERTY_ID);
      resourceSet.addAll(workflowFetcher.fetchWorkflows(requestedIds,
          clusterName, workflowId));
    }
    return resourceSet;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException();
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

  @Override
  public Map<Type,String> getKeyPropertyIds() {
    return keyPropertyIds;
  }

  /**
   * Simple interface for fetching workflows from db.
   */
  public interface WorkflowFetcher {
    /**
     * Fetch workflow resources.
     * 
     * @param requestedIds
     *          fields to pull from db
     * @param clusterName
     *          the cluster name
     * @param workflowId
     *          the workflow id
     * @return a set of workflow resources
     */
    Set<Resource> fetchWorkflows(Set<String> requestedIds,
                                 String clusterName, String workflowId);
  }

  /**
   * A workflow fetcher that queries a postgres workflow table.
   */
  protected class PostgresWorkflowFetcher implements WorkflowFetcher {
    private static final String WORKFLOW_TABLE_NAME = "workflow";
    private ConnectionFactory connectionFactory;
    private Connection db;
    private PreparedStatement ps;

    /**
     * Create a postgres workflow fetcher that uses a given connection factory.
     * 
     * @param connectionFactory
     *          a connection factory
     */
    public PostgresWorkflowFetcher(ConnectionFactory connectionFactory) {
      this.connectionFactory = connectionFactory;
      this.db = null;
      this.ps = null;
    }

    protected ResultSet getResultSet(Set<String> requestedIds, String workflowId)
        throws SQLException {
      db = null;
      ps = null;
      db = connectionFactory.getConnection();
      if (workflowId == null) {
        ps = db.prepareStatement("SELECT " + getDBFieldString(requestedIds)
            + " FROM " + WORKFLOW_TABLE_NAME);
      } else {
        ps = db.prepareStatement("SELECT " + getDBFieldString(requestedIds)
            + " FROM " + WORKFLOW_TABLE_NAME + " WHERE "
            + WorkflowFields.WORKFLOWID + " = ?");
        ps.setString(1, workflowId);
      }
      return ps.executeQuery();
    }

    protected void close() {
      if (ps != null)
        try {
          ps.close();
        } catch (SQLException e) {
          LOG.error("Exception while closing statment", e);
        }

      if (db != null)
        try {
          db.close();
        } catch (SQLException e) {
          LOG.error("Exception while closing connection", e);
        }
    }

    @Override
    public Set<Resource> fetchWorkflows(Set<String> requestedIds,
        String clusterName, String workflowId) {
      Set<Resource> workflows = new HashSet<>();
      ResultSet rs = null;
      try {
        rs = getResultSet(requestedIds, workflowId);
        while (rs.next()) {
          Resource resource = new ResourceImpl(Resource.Type.Workflow);
          setResourceProperty(resource, WORKFLOW_CLUSTER_NAME_PROPERTY_ID,
              clusterName, requestedIds);
          setString(resource, WORKFLOW_ID_PROPERTY_ID, rs, requestedIds);
          setString(resource, WORKFLOW_NAME_PROPERTY_ID, rs, requestedIds);
          setString(resource, WORKFLOW_USER_NAME_PROPERTY_ID, rs, requestedIds);
          setLong(resource, WORKFLOW_START_TIME_PROPERTY_ID, rs, requestedIds);
          setLong(resource, WORKFLOW_LAST_UPDATE_TIME_PROPERTY_ID, rs,
              requestedIds);
          setLong(resource, WORKFLOW_ELAPSED_TIME_PROPERTY_ID, rs, requestedIds);
          setLong(resource, WORKFLOW_INPUT_BYTES_PROPERTY_ID, rs, requestedIds);
          setLong(resource, WORKFLOW_OUTPUT_BYTES_PROPERTY_ID, rs, requestedIds);
          setInt(resource, WORKFLOW_NUM_JOBS_TOTAL_PROPERTY_ID, rs,
              requestedIds);
          setInt(resource, WORKFLOW_NUM_JOBS_COMPLETED_PROPERTY_ID, rs,
              requestedIds);
          setString(resource, WORKFLOW_PARENT_ID_PROPERTY_ID, rs, requestedIds);
          setString(resource, WORKFLOW_CONTEXT_PROPERTY_ID, rs, requestedIds);
          workflows.add(resource);
        }
      } catch (SQLException e) {
        if (LOG.isDebugEnabled())
          LOG.debug("Caught exception getting resource.", e);
        return Collections.emptySet();
      } finally {
        if (rs != null)
          try {
            rs.close();
          } catch (SQLException e) {
            LOG.error("Exception while closing ResultSet", e);
          }

        close();
      }
      return workflows;
    }
  }

  /**
   * Enumeration of db fields for the workflow table.
   */
  enum WorkflowFields {
    WORKFLOWID,
    WORKFLOWNAME,
    USERNAME,
    STARTTIME,
    LASTUPDATETIME,
    DURATION,
    NUMJOBSTOTAL,
    NUMJOBSCOMPLETED,
    INPUTBYTES,
    OUTPUTBYTES,
    PARENTWORKFLOWID,
    WORKFLOWCONTEXT
  }

  @Override
  protected Map<String,WorkflowFields> getDBFieldMap() {
    Map<String,WorkflowFields> dbFields = new HashMap<>();
    dbFields.put(WORKFLOW_ID_PROPERTY_ID, WorkflowFields.WORKFLOWID);
    dbFields.put(WORKFLOW_NAME_PROPERTY_ID, WorkflowFields.WORKFLOWNAME);
    dbFields.put(WORKFLOW_USER_NAME_PROPERTY_ID, WorkflowFields.USERNAME);
    dbFields.put(WORKFLOW_START_TIME_PROPERTY_ID, WorkflowFields.STARTTIME);
    dbFields.put(WORKFLOW_LAST_UPDATE_TIME_PROPERTY_ID,
        WorkflowFields.LASTUPDATETIME);
    dbFields.put(WORKFLOW_ELAPSED_TIME_PROPERTY_ID, WorkflowFields.DURATION);
    dbFields.put(WORKFLOW_INPUT_BYTES_PROPERTY_ID, WorkflowFields.INPUTBYTES);
    dbFields.put(WORKFLOW_OUTPUT_BYTES_PROPERTY_ID, WorkflowFields.OUTPUTBYTES);
    dbFields.put(WORKFLOW_NUM_JOBS_TOTAL_PROPERTY_ID,
        WorkflowFields.NUMJOBSTOTAL);
    dbFields.put(WORKFLOW_NUM_JOBS_COMPLETED_PROPERTY_ID,
        WorkflowFields.NUMJOBSCOMPLETED);
    dbFields.put(WORKFLOW_PARENT_ID_PROPERTY_ID,
        WorkflowFields.PARENTWORKFLOWID);
    dbFields.put(WORKFLOW_CONTEXT_PROPERTY_ID, WorkflowFields.WORKFLOWCONTEXT);
    return dbFields;
  }
}

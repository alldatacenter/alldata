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
import com.google.common.collect.ImmutableSet;

/**
 * Resource provider for task attempt resources.
 */
public class TaskAttemptResourceProvider extends
    AbstractJDBCResourceProvider<TaskAttemptResourceProvider.TaskAttemptFields> {
  private static final Logger LOG = LoggerFactory.getLogger(TaskAttemptResourceProvider.class);

  protected static final String TASK_ATTEMPT_CLUSTER_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("TaskAttempt", "cluster_name");
  protected static final String TASK_ATTEMPT_WORKFLOW_ID_PROPERTY_ID = PropertyHelper
      .getPropertyId("TaskAttempt", "workflow_id");
  protected static final String TASK_ATTEMPT_JOB_ID_PROPERTY_ID = PropertyHelper
      .getPropertyId("TaskAttempt", "job_id");
  protected static final String TASK_ATTEMPT_ID_PROPERTY_ID = PropertyHelper
      .getPropertyId("TaskAttempt", "task_attempt_id");
  protected static final String TASK_ATTEMPT_TYPE_PROPERTY_ID = PropertyHelper
      .getPropertyId("TaskAttempt", "type");
  protected static final String TASK_ATTEMPT_START_TIME_PROPERTY_ID = PropertyHelper
      .getPropertyId("TaskAttempt", "start_time");
  protected static final String TASK_ATTEMPT_FINISH_TIME_PROPERTY_ID = PropertyHelper
      .getPropertyId("TaskAttempt", "finish_time");
  protected static final String TASK_ATTEMPT_MAP_FINISH_TIME_PROPERTY_ID = PropertyHelper
      .getPropertyId("TaskAttempt", "map_finish_time");
  protected static final String TASK_ATTEMPT_SHUFFLE_FINISH_TIME_PROPERTY_ID = PropertyHelper
      .getPropertyId("TaskAttempt", "shuffle_finish_time");
  protected static final String TASK_ATTEMPT_SORT_FINISH_TIME_PROPERTY_ID = PropertyHelper
      .getPropertyId("TaskAttempt", "sort_finish_fime");
  protected static final String TASK_ATTEMPT_INPUT_BYTES_PROPERTY_ID = PropertyHelper
      .getPropertyId("TaskAttempt", "input_bytes");
  protected static final String TASK_ATTEMPT_OUTPUT_BYTES_PROPERTY_ID = PropertyHelper
      .getPropertyId("TaskAttempt", "output_bytes");
  protected static final String TASK_ATTEMPT_STATUS_PROPERTY_ID = PropertyHelper
      .getPropertyId("TaskAttempt", "status");
  protected static final String TASK_ATTEMPT_LOCALITY_PROPERTY_ID = PropertyHelper
      .getPropertyId("TaskAttempt", "locality");

  protected TaskAttemptFetcher taskAttemptFetcher;

  /**
   * The key property ids for a TaskAttempt resource.
   */
  protected static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Type.Cluster, TASK_ATTEMPT_CLUSTER_NAME_PROPERTY_ID)
      .put(Type.Workflow, TASK_ATTEMPT_WORKFLOW_ID_PROPERTY_ID)
      .put(Type.Job, TASK_ATTEMPT_JOB_ID_PROPERTY_ID)
      .put(Type.TaskAttempt, TASK_ATTEMPT_ID_PROPERTY_ID)
      .build();

  /**
   * The property ids for a TaskAttempt resource.
   */
  protected static final Set<String> propertyIds = ImmutableSet.of(
      TASK_ATTEMPT_CLUSTER_NAME_PROPERTY_ID,
      TASK_ATTEMPT_WORKFLOW_ID_PROPERTY_ID,
      TASK_ATTEMPT_JOB_ID_PROPERTY_ID,
      TASK_ATTEMPT_ID_PROPERTY_ID,
      TASK_ATTEMPT_TYPE_PROPERTY_ID,
      TASK_ATTEMPT_START_TIME_PROPERTY_ID,
      TASK_ATTEMPT_FINISH_TIME_PROPERTY_ID,
      TASK_ATTEMPT_MAP_FINISH_TIME_PROPERTY_ID,
      TASK_ATTEMPT_SHUFFLE_FINISH_TIME_PROPERTY_ID,
      TASK_ATTEMPT_SORT_FINISH_TIME_PROPERTY_ID,
      TASK_ATTEMPT_INPUT_BYTES_PROPERTY_ID,
      TASK_ATTEMPT_OUTPUT_BYTES_PROPERTY_ID,
      TASK_ATTEMPT_STATUS_PROPERTY_ID,
      TASK_ATTEMPT_LOCALITY_PROPERTY_ID);

  /**
   * Create a new task attempt resource provider.
   */
  protected TaskAttemptResourceProvider() {
    super(propertyIds, keyPropertyIds);
    taskAttemptFetcher = new PostgresTaskAttemptFetcher(
        new JobHistoryPostgresConnectionFactory());
  }

  /**
   * Create a new task attempt resource provider.
   * 
   * @param propertyIds
   *          the property ids
   * @param keyPropertyIds
   *          the key property ids
   * @param taskAttemptFetcher
   *          task attempt fetcher
   */
  protected TaskAttemptResourceProvider(Set<String> propertyIds,
      Map<Type,String> keyPropertyIds, TaskAttemptFetcher taskAttemptFetcher) {
    super(propertyIds, keyPropertyIds);
    this.taskAttemptFetcher = taskAttemptFetcher;
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
          .get(TASK_ATTEMPT_CLUSTER_NAME_PROPERTY_ID);
      String workflowId = (String) predicateProperties
          .get(TASK_ATTEMPT_WORKFLOW_ID_PROPERTY_ID);
      String jobId = (String) predicateProperties
          .get(TASK_ATTEMPT_JOB_ID_PROPERTY_ID);
      String taskAttemptId = (String) predicateProperties
          .get(TASK_ATTEMPT_ID_PROPERTY_ID);
      resourceSet.addAll(taskAttemptFetcher.fetchTaskAttemptDetails(
          requestedIds, clusterName, workflowId, jobId, taskAttemptId));
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
   * Simple interface for fetching task attempts from db.
   */
  public interface TaskAttemptFetcher {
    /**
     * Fetch task attempt resources
     * 
     * @param requestedIds
     *          fields to pull from db
     * @param clusterName
     *          the cluster name
     * @param workflowId
     *          the workflow id
     * @param jobId
     *          the job id
     * @param taskAttemptId
     *          the task attempt id
     * @return a set of task attempt resources
     */
    Set<Resource> fetchTaskAttemptDetails(Set<String> requestedIds,
                                          String clusterName, String workflowId, String jobId,
                                          String taskAttemptId);
  }

  /**
   * A task attempt fetcher that queries a postgres task attempt table.
   */
  protected class PostgresTaskAttemptFetcher implements TaskAttemptFetcher {
    private static final String TASK_ATTEMPT_TABLE_NAME = "taskattempt";
    private ConnectionFactory connectionFactory;
    Connection db;
    PreparedStatement ps;

    /**
     * Create a postgres task attempt fetcher that uses a given connection
     * factory.
     * 
     * @param connectionFactory
     *          a connection factory
     */
    public PostgresTaskAttemptFetcher(ConnectionFactory connectionFactory) {
      this.connectionFactory = connectionFactory;
      this.db = null;
      this.ps = null;
    }

    protected ResultSet getResultSet(Set<String> requestedIds,
        String workflowId, String jobId, String taskAttemptId)
        throws SQLException {
      db = null;
      ps = null;
      db = connectionFactory.getConnection();
      if (taskAttemptId == null) {
        ps = db.prepareStatement("SELECT " + getDBFieldString(requestedIds)
            + " FROM " + TASK_ATTEMPT_TABLE_NAME + " WHERE "
            + TaskAttemptFields.JOBID + " = ? ");
        ps.setString(1, jobId);
      } else {
        ps = db.prepareStatement("SELECT " + getDBFieldString(requestedIds)
            + " FROM " + TASK_ATTEMPT_TABLE_NAME + " WHERE "
            + TaskAttemptFields.TASKATTEMPTID + " = ? ");
        ps.setString(1, taskAttemptId);
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
    public Set<Resource> fetchTaskAttemptDetails(Set<String> requestedIds,
        String clusterName, String workflowId, String jobId,
        String taskAttemptId) {
      Set<Resource> taskAttempts = new HashSet<>();
      ResultSet rs = null;
      try {
        rs = getResultSet(requestedIds, workflowId, jobId, taskAttemptId);
        while (rs.next()) {
          Resource resource = new ResourceImpl(Resource.Type.TaskAttempt);
          setResourceProperty(resource, TASK_ATTEMPT_CLUSTER_NAME_PROPERTY_ID,
              clusterName, requestedIds);
          setResourceProperty(resource, TASK_ATTEMPT_WORKFLOW_ID_PROPERTY_ID,
              workflowId, requestedIds);
          setString(resource, TASK_ATTEMPT_JOB_ID_PROPERTY_ID, rs, requestedIds);
          setString(resource, TASK_ATTEMPT_ID_PROPERTY_ID, rs, requestedIds);
          setString(resource, TASK_ATTEMPT_TYPE_PROPERTY_ID, rs, requestedIds);
          setLong(resource, TASK_ATTEMPT_START_TIME_PROPERTY_ID, rs,
              requestedIds);
          setLong(resource, TASK_ATTEMPT_FINISH_TIME_PROPERTY_ID, rs,
              requestedIds);
          setLong(resource, TASK_ATTEMPT_MAP_FINISH_TIME_PROPERTY_ID, rs,
              requestedIds);
          setLong(resource, TASK_ATTEMPT_SHUFFLE_FINISH_TIME_PROPERTY_ID, rs,
              requestedIds);
          setLong(resource, TASK_ATTEMPT_SORT_FINISH_TIME_PROPERTY_ID, rs,
              requestedIds);
          setLong(resource, TASK_ATTEMPT_INPUT_BYTES_PROPERTY_ID, rs,
              requestedIds);
          setLong(resource, TASK_ATTEMPT_OUTPUT_BYTES_PROPERTY_ID, rs,
              requestedIds);
          setString(resource, TASK_ATTEMPT_STATUS_PROPERTY_ID, rs, requestedIds);
          setString(resource, TASK_ATTEMPT_LOCALITY_PROPERTY_ID, rs,
              requestedIds);
          taskAttempts.add(resource);
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
      return taskAttempts;
    }
  }

  /**
   * Enumeration of db fields for the task attempt table.
   */
  enum TaskAttemptFields {
    JOBID,
    TASKATTEMPTID,
    TASKTYPE,
    STARTTIME,
    FINISHTIME,
    MAPFINISHTIME,
    SHUFFLEFINISHTIME,
    SORTFINISHTIME,
    INPUTBYTES,
    OUTPUTBYTES,
    STATUS,
    LOCALITY
  }

  @Override
  protected Map<String,TaskAttemptFields> getDBFieldMap() {
    Map<String,TaskAttemptFields> dbFields = new HashMap<>();
    dbFields.put(TASK_ATTEMPT_JOB_ID_PROPERTY_ID, TaskAttemptFields.JOBID);
    dbFields.put(TASK_ATTEMPT_ID_PROPERTY_ID, TaskAttemptFields.TASKATTEMPTID);
    dbFields.put(TASK_ATTEMPT_TYPE_PROPERTY_ID, TaskAttemptFields.TASKTYPE);
    dbFields.put(TASK_ATTEMPT_START_TIME_PROPERTY_ID,
        TaskAttemptFields.STARTTIME);
    dbFields.put(TASK_ATTEMPT_FINISH_TIME_PROPERTY_ID,
        TaskAttemptFields.FINISHTIME);
    dbFields.put(TASK_ATTEMPT_MAP_FINISH_TIME_PROPERTY_ID,
        TaskAttemptFields.MAPFINISHTIME);
    dbFields.put(TASK_ATTEMPT_SHUFFLE_FINISH_TIME_PROPERTY_ID,
        TaskAttemptFields.SHUFFLEFINISHTIME);
    dbFields.put(TASK_ATTEMPT_SORT_FINISH_TIME_PROPERTY_ID,
        TaskAttemptFields.SORTFINISHTIME);
    dbFields.put(TASK_ATTEMPT_INPUT_BYTES_PROPERTY_ID,
        TaskAttemptFields.INPUTBYTES);
    dbFields.put(TASK_ATTEMPT_OUTPUT_BYTES_PROPERTY_ID,
        TaskAttemptFields.OUTPUTBYTES);
    dbFields.put(TASK_ATTEMPT_STATUS_PROPERTY_ID, TaskAttemptFields.STATUS);
    dbFields.put(TASK_ATTEMPT_LOCALITY_PROPERTY_ID, TaskAttemptFields.LOCALITY);
    return dbFields;
  }
}

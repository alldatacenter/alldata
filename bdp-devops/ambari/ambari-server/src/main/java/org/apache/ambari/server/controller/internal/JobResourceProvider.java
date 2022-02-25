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
 * Resource provider for job resources.
 */
public class JobResourceProvider extends
    AbstractJDBCResourceProvider<JobResourceProvider.JobFields> {
  private static final Logger LOG = LoggerFactory.getLogger(JobResourceProvider.class);

  protected static final String JOB_CLUSTER_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Job", "cluster_name");
  protected static final String JOB_WORKFLOW_ID_PROPERTY_ID = PropertyHelper
      .getPropertyId("Job", "workflow_id");
  protected static final String JOB_ID_PROPERTY_ID = PropertyHelper
      .getPropertyId("Job", "job_id");
  protected static final String JOB_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Job", "name");
  protected static final String JOB_STATUS_PROPERTY_ID = PropertyHelper
      .getPropertyId("Job", "status");
  protected static final String JOB_USER_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Job", "user_name");
  protected static final String JOB_SUBMIT_TIME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Job", "submit_time");
  protected static final String JOB_ELAPSED_TIME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Job", "elapsed_time");
  protected static final String JOB_MAPS_PROPERTY_ID = PropertyHelper
      .getPropertyId("Job", "maps");
  protected static final String JOB_REDUCES_PROPERTY_ID = PropertyHelper
      .getPropertyId("Job", "reduces");
  protected static final String JOB_INPUT_BYTES_PROPERTY_ID = PropertyHelper
      .getPropertyId("Job", "input_bytes");
  protected static final String JOB_OUTPUT_BYTES_PROPERTY_ID = PropertyHelper
      .getPropertyId("Job", "output_bytes");
  protected static final String JOB_CONF_PATH_PROPERTY_ID = PropertyHelper
      .getPropertyId("Job", "conf_path");
  protected static final String JOB_WORKFLOW_ENTITY_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("Job", "workflow_entity_name");

  protected JobFetcher jobFetcher;

  /**
   * The key property ids for a Job resource.
   */
  protected static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Type.Cluster, JOB_CLUSTER_NAME_PROPERTY_ID)
      .put(Type.Workflow, JOB_WORKFLOW_ID_PROPERTY_ID)
      .put(Type.Job, JOB_ID_PROPERTY_ID)
      .build();

  /**
   * The property ids for a Job resource.
   */
  protected static final Set<String> propertyIds = ImmutableSet.of(
      JOB_CLUSTER_NAME_PROPERTY_ID,
      JOB_WORKFLOW_ID_PROPERTY_ID,
      JOB_ID_PROPERTY_ID,
      JOB_NAME_PROPERTY_ID,
      JOB_STATUS_PROPERTY_ID,
      JOB_USER_NAME_PROPERTY_ID,
      JOB_SUBMIT_TIME_PROPERTY_ID,
      JOB_ELAPSED_TIME_PROPERTY_ID,
      JOB_MAPS_PROPERTY_ID,
      JOB_REDUCES_PROPERTY_ID,
      JOB_INPUT_BYTES_PROPERTY_ID,
      JOB_OUTPUT_BYTES_PROPERTY_ID,
      JOB_CONF_PATH_PROPERTY_ID,
      JOB_WORKFLOW_ENTITY_NAME_PROPERTY_ID);

  /**
   * Create a new job resource provider.
   */
  protected JobResourceProvider() {
    super(propertyIds, keyPropertyIds);
    jobFetcher = new PostgresJobFetcher(
        new JobHistoryPostgresConnectionFactory());
  }

  /**
   * Create a new job resource provider.
   * 
   * @param jobFetcher
   *          job fetcher
   */
  protected JobResourceProvider(JobFetcher jobFetcher) {
    super(propertyIds, keyPropertyIds);
    this.jobFetcher = jobFetcher;
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
          .get(JOB_CLUSTER_NAME_PROPERTY_ID);
      String workflowId = (String) predicateProperties
          .get(JOB_WORKFLOW_ID_PROPERTY_ID);
      String jobId = (String) predicateProperties.get(JOB_ID_PROPERTY_ID);
      resourceSet.addAll(jobFetcher.fetchJobDetails(requestedIds, clusterName,
          workflowId, jobId));
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
   * Simple interface for fetching jobs from db.
   */
  public interface JobFetcher {
    /**
     * Fetch job resources.
     * 
     * @param requestedIds
     *          fields to pull from db
     * @param clusterName
     *          the cluster name
     * @param workflowId
     *          the workflow id
     * @param jobId
     *          the job id
     * @return a set of job resources
     */
    Set<Resource> fetchJobDetails(Set<String> requestedIds,
                                  String clusterName, String workflowId, String jobId);
  }

  /**
   * A job fetcher that queries a postgres job table.
   */
  protected class PostgresJobFetcher implements JobFetcher {
    private static final String JOB_TABLE_NAME = "job";
    private ConnectionFactory connectionFactory;
    Connection db;
    PreparedStatement ps;

    /**
     * Create a postgres job fetcher that uses a given connection factory.
     * 
     * @param connectionFactory
     *          a connection factory
     */
    public PostgresJobFetcher(ConnectionFactory connectionFactory) {
      this.connectionFactory = connectionFactory;
      this.db = null;
      this.ps = null;
    }

    protected ResultSet getResultSet(Set<String> requestedIds,
        String workflowId, String jobId) throws SQLException {
      db = null;
      ps = null;
      db = connectionFactory.getConnection();
      String fields = getDBFieldString(requestedIds);
      if (requestedIds.contains(JOB_ELAPSED_TIME_PROPERTY_ID)
          && !requestedIds.contains(JOB_SUBMIT_TIME_PROPERTY_ID))
        fields += "," + getDBField(JOB_SUBMIT_TIME_PROPERTY_ID);
      if (jobId == null) {
        ps = db.prepareStatement("SELECT " + fields + " FROM " + JOB_TABLE_NAME
            + " WHERE " + JobFields.WORKFLOWID + " = ?");
        ps.setString(1, workflowId);
      } else {
        ps = db.prepareStatement("SELECT " + fields + " FROM " + JOB_TABLE_NAME
            + " WHERE " + JobFields.JOBID + " = ?");
        ps.setString(1, jobId);
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
    public Set<Resource> fetchJobDetails(Set<String> requestedIds,
        String clusterName, String workflowId, String jobId) {
      Set<Resource> jobs = new HashSet<>();
      ResultSet rs = null;
      try {
        rs = getResultSet(requestedIds, workflowId, jobId);
        while (rs.next()) {
          Resource resource = new ResourceImpl(Resource.Type.Job);
          setResourceProperty(resource, JOB_CLUSTER_NAME_PROPERTY_ID,
              clusterName, requestedIds);
          setString(resource, JOB_ID_PROPERTY_ID, rs, requestedIds);
          setString(resource, JOB_NAME_PROPERTY_ID, rs, requestedIds);
          setString(resource, JOB_STATUS_PROPERTY_ID, rs, requestedIds);
          setString(resource, JOB_USER_NAME_PROPERTY_ID, rs, requestedIds);
          if (requestedIds.contains(JOB_SUBMIT_TIME_PROPERTY_ID)
              || requestedIds.contains(JOB_ELAPSED_TIME_PROPERTY_ID)) {
            long submitTime = rs.getLong(JobFields.SUBMITTIME.toString());
            if (requestedIds.contains(JOB_SUBMIT_TIME_PROPERTY_ID))
              setResourceProperty(resource, JOB_SUBMIT_TIME_PROPERTY_ID,
                  submitTime, requestedIds);
            if (requestedIds.contains(JOB_ELAPSED_TIME_PROPERTY_ID)) {
              long finishTime = rs.getLong(JobFields.FINISHTIME.toString());
              if (finishTime > submitTime)
                setResourceProperty(resource, JOB_ELAPSED_TIME_PROPERTY_ID,
                    finishTime - submitTime, requestedIds);
              else
                setResourceProperty(resource, JOB_ELAPSED_TIME_PROPERTY_ID, 0l,
                    requestedIds);
            }
          }
          setInt(resource, JOB_MAPS_PROPERTY_ID, rs, requestedIds);
          setInt(resource, JOB_REDUCES_PROPERTY_ID, rs, requestedIds);
          setLong(resource, JOB_INPUT_BYTES_PROPERTY_ID, rs, requestedIds);
          setLong(resource, JOB_OUTPUT_BYTES_PROPERTY_ID, rs, requestedIds);
          setString(resource, JOB_CONF_PATH_PROPERTY_ID, rs, requestedIds);
          setString(resource, JOB_WORKFLOW_ID_PROPERTY_ID, rs, requestedIds);
          setString(resource, JOB_WORKFLOW_ENTITY_NAME_PROPERTY_ID, rs,
              requestedIds);
          jobs.add(resource);
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
      return jobs;
    }
  }

  /**
   * Enumeration of db fields for the job table.
   */
  enum JobFields {
    JOBID,
    JOBNAME,
    STATUS,
    USERNAME,
    SUBMITTIME,
    FINISHTIME,
    MAPS,
    REDUCES,
    INPUTBYTES,
    OUTPUTBYTES,
    CONFPATH,
    WORKFLOWID,
    WORKFLOWENTITYNAME
  }

  @Override
  protected Map<String,JobFields> getDBFieldMap() {
    Map<String,JobFields> dbFields = new HashMap<>();
    dbFields.put(JOB_WORKFLOW_ID_PROPERTY_ID, JobFields.WORKFLOWID);
    dbFields.put(JOB_ID_PROPERTY_ID, JobFields.JOBID);
    dbFields.put(JOB_NAME_PROPERTY_ID, JobFields.JOBNAME);
    dbFields.put(JOB_STATUS_PROPERTY_ID, JobFields.STATUS);
    dbFields.put(JOB_USER_NAME_PROPERTY_ID, JobFields.USERNAME);
    dbFields.put(JOB_SUBMIT_TIME_PROPERTY_ID, JobFields.SUBMITTIME);
    dbFields.put(JOB_ELAPSED_TIME_PROPERTY_ID, JobFields.FINISHTIME);
    dbFields.put(JOB_MAPS_PROPERTY_ID, JobFields.MAPS);
    dbFields.put(JOB_REDUCES_PROPERTY_ID, JobFields.REDUCES);
    dbFields.put(JOB_INPUT_BYTES_PROPERTY_ID, JobFields.INPUTBYTES);
    dbFields.put(JOB_OUTPUT_BYTES_PROPERTY_ID, JobFields.OUTPUTBYTES);
    dbFields.put(JOB_CONF_PATH_PROPERTY_ID, JobFields.CONFPATH);
    dbFields.put(JOB_WORKFLOW_ENTITY_NAME_PROPERTY_ID,
        JobFields.WORKFLOWENTITYNAME);
    return dbFields;
  }
}

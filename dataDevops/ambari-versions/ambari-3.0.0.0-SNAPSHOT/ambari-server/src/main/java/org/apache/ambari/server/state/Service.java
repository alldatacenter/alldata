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

package org.apache.ambari.server.state;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;

public interface Service {

  String getName();

  String getDisplayName();

  long getClusterId();

  Cluster getCluster();

  ServiceComponent getServiceComponent(String componentName)
      throws AmbariException;

  Map<String, ServiceComponent> getServiceComponents();

  Set<String> getServiceHosts();

  void addServiceComponents(Map<String, ServiceComponent> components)
      throws AmbariException;

  void addServiceComponent(ServiceComponent component)
      throws AmbariException;

  State getDesiredState();

  void setDesiredState(State state);

  StackId getDesiredStackId();

  ServiceResponse convertToResponse();

  void debugDump(StringBuilder sb);

  ServiceComponent addServiceComponent(String serviceComponentName)
      throws AmbariException;

  /**
   * Find out whether the service and its components
   * are in a state that it can be removed from a cluster
   * @return
   */
  boolean canBeRemoved();

  void deleteAllComponents(DeleteHostComponentStatusMetaData deleteMetaData);

  void deleteServiceComponent(String componentName, DeleteHostComponentStatusMetaData deleteMetaData)
      throws AmbariException;

  boolean isClientOnlyService();

  void delete(DeleteHostComponentStatusMetaData deleteMetaData);

  /**
   * Sets the maintenance state for the service
   * @param state the state
   */
  void setMaintenanceState(MaintenanceState state);

  /**
   * @return the maintenance state
   */
  MaintenanceState getMaintenanceState();

  /**
   * Tests to see if Kerberos is enabled for this service using the Kerberos enabled test metadata
   * and the existing cluster configurations.
   *
   * @return <code>true</code>. if it is determined that Kerberos is enabled for this service; <code>false</code>, otherwise
   * @see #isKerberosEnabled(Map)
   */
  boolean isKerberosEnabled();

  /**
   * Tests to see if Kerberos is enabled for this service using the Kerberos enabled test metadata
   * and the supplied configurations map.
   *
   * @param configurations a map of configurations to use for the test
   * @return <code>true</code>. if it is determined that Kerberos is enabled for this service; <code>false</code>, otherwise
   * @see #isKerberosEnabled()
   */
  boolean isKerberosEnabled(Map<String, Map<String, String>> configurations);

  /**
   * Refresh Service info due to current stack
   * @throws AmbariException
   */
  void updateServiceInfo() throws AmbariException;


  /**
   * Get a true or false value specifying
   * whether credential store is supported by this service.
   * @return true or false
   */
  boolean isCredentialStoreSupported();

  /**
   * Get a true or false value specifying
   * whether credential store is required by this service.
   * @return true or false
   */
  boolean isCredentialStoreRequired();

  /**
   * Get a true or false value specifying whether
   * credential store use is enabled for this service.
   *
   * @return true or false
   */
  boolean isCredentialStoreEnabled();

  /**
   * Set a true or false value specifying whether this
   * service is to be enabled for credential store use.
   *
   * @param credentialStoreEnabled - true or false
   */
  void setCredentialStoreEnabled(boolean credentialStoreEnabled);

  /**
   * @return
   */
  RepositoryVersionEntity getDesiredRepositoryVersion();

  /**
   * @param desiredRepositoryVersion
   */
  void setDesiredRepositoryVersion(RepositoryVersionEntity desiredRepositoryVersion);

  /**
   * Gets the repository for the desired version of this service by consulting
   * the repository states of all known components.
   */
  RepositoryVersionState getRepositoryState();

  enum Type {
    HDFS,
    GLUSTERFS,
    MAPREDUCE,
    HBASE,
    HIVE,
    OOZIE,
    WEBHCAT,
    SQOOP,
    GANGLIA,
    ZOOKEEPER,
    PIG,
    FLUME,
    YARN,
    MAPREDUCE2,
    AMBARI_METRICS,
    KERBEROS
  }
}

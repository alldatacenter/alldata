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

import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;

public interface ServiceFactory {

  /**
   * Creates a new service in memory and then persists it to the database.
   *
   * @param cluster
   *          the cluster the service is for (not {@code null).
   * @param serviceName
   *          the name of the service (not {@code null).
   * @param desiredRepositoryVersion
   *          the repository version of the service (not {@code null).
   * @return
   */
  Service createNew(Cluster cluster, String serviceName,
      RepositoryVersionEntity desiredRepositoryVersion);

  /**
   * Creates an in-memory representation of a service from an existing database
   * object.
   *
   * @param cluster
   *          the cluster the service is installed in (not {@code null).
   * @param serviceEntity
   *          the entity the existing database entry (not {@code null).
   * @return
   */
  Service createExisting(Cluster cluster, ClusterServiceEntity serviceEntity);
}

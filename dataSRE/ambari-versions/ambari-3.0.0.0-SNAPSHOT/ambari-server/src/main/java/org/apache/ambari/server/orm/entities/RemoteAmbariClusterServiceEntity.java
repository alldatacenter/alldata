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

package org.apache.ambari.server.orm.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * Remote Ambari Service to Remote Cluster Mapping
 */
@Table(name = "remoteambariclusterservice")
@TableGenerator(name = "remote_cluster_service_id_generator",
  table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
  , pkColumnValue = "remote_cluster_service_id_seq"
  , initialValue = 1
)
@Entity
public class RemoteAmbariClusterServiceEntity {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "remote_cluster_service_id_generator")
  private Long id;

  @Column(name = "service_name", nullable = false, insertable = true, updatable = false)
  private String serviceName;

  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false)
  private RemoteAmbariClusterEntity cluster;

  /**
   * Get Id
   *
   * @return id
   */
  public Long getId() {
    return id;
  }

  /**
   * Set id for the service
   *
   * @param id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Get cluster attached to the service
   *
   * @return cluster
   */
  public RemoteAmbariClusterEntity getCluster() {
    return cluster;
  }

  /**
   * Set cluster for the service
   *
   * @param cluster
   */
  public void setCluster(RemoteAmbariClusterEntity cluster) {
    this.cluster = cluster;
  }

  /**
   * Get service name
   *
   * @return service name
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Set service name
   *
   * @param serviceName
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }
}

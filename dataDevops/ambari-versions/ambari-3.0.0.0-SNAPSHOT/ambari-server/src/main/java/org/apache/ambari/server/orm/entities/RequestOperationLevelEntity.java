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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "requestoperationlevel")
@TableGenerator(name = "operation_level_id_generator",
  table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
  , pkColumnValue = "operation_level_id_seq"
  , initialValue = 1
)
@NamedQueries({
    @NamedQuery(name = "requestOperationLevelByHostId", query =
        "SELECT requestOperationLevel FROM RequestOperationLevelEntity requestOperationLevel " +
            "WHERE requestOperationLevel.hostId=:hostId"),
    @NamedQuery(name = "RequestOperationLevelEntity.removeByRequestIds",
        query = "DELETE FROM RequestOperationLevelEntity requestOperationLevel WHERE requestOperationLevel.requestId IN :requestIds")
})
public class RequestOperationLevelEntity {

  @Id
  @Column(name = "operation_level_id", nullable = false, insertable = true, updatable = true)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "operation_level_id_generator")
  private Long operationLevelId;

  @Column(name = "request_id", nullable = false, insertable = true, updatable = true)
  private Long requestId;

  @OneToOne
  @JoinColumn(name = "request_id", referencedColumnName = "request_id", nullable = false, insertable = false, updatable = false)
  private RequestEntity requestEntity;

  public Long getOperationLevelId() {
    return operationLevelId;
  }

  public void setOperationLevelId(Long operationLevelId) {
    this.operationLevelId = operationLevelId;
  }

  @Column(name = "level_name")
  @Basic
  private String level;

  @Column(name = "cluster_name")
  @Basic
  private String clusterName;

  @Column(name = "service_name")
  @Basic
  private String serviceName;

  @Column(name = "host_component_name")
  @Basic
  private String hostComponentName;

  @Column(name = "host_id", nullable=true, insertable=true, updatable=true) // Notice that allowed to be null
  @Basic
  private Long hostId;

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getHostComponentName() {
    return hostComponentName;
  }

  public void setHostComponentName(String hostComponentName) {
    this.hostComponentName = hostComponentName;
  }

  public Long getHostId() {
    return hostId;
  }

  public void setHostId(Long hostId) {
    this.hostId = hostId;
  }

  public Long getRequestId() {
    return requestId;
  }

  public void setRequestId(Long requestId) {
    this.requestId = requestId;
  }

  public RequestEntity getRequestEntity() {
    return requestEntity;
  }

  public void setRequestEntity(RequestEntity request) {
    this.requestEntity = request;
  }
}

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
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "requestresourcefilter")
@TableGenerator(name = "resourcefilter_id_generator",
  table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
  , pkColumnValue = "resourcefilter_id_seq"
  , initialValue = 1
)
@NamedQueries({
  @NamedQuery(name = "RequestResourceFilterEntity.removeByRequestIds", query = "DELETE FROM RequestResourceFilterEntity filter WHERE filter.requestId IN :requestIds")
})
public class RequestResourceFilterEntity {

  @Id
  @Column(name = "filter_id", nullable = false, insertable = true, updatable = true)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "resourcefilter_id_generator")
  private Long filterId;

  @Column(name = "request_id", nullable = false, insertable = true, updatable = true)
  private Long requestId;

  @Column(name = "service_name")
  @Basic
  private String serviceName;

  @Column(name = "component_name")
  @Basic
  private String componentName;

  @Column(name = "hosts")
  @Lob
  private byte[] hosts;

  @ManyToOne
  @JoinColumn(name = "request_id", referencedColumnName = "request_id", nullable = false, insertable = false, updatable = false)
  private RequestEntity requestEntity;

  public Long getFilterId() {
    return filterId;
  }

  public void setFilterId(Long filterId) {
    this.filterId = filterId;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public String getHosts() {
    return hosts != null ? new String(hosts) : null;
  }

  public void setHosts(String hosts) {
    this.hosts = hosts != null ? hosts.getBytes() : null;
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

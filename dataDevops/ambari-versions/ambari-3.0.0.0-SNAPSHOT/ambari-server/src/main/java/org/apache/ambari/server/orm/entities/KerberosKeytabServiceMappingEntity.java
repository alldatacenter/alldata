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
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "kkp_mapping_service")
public class KerberosKeytabServiceMappingEntity {
  @Id
  @Column(name = "kkp_id", nullable = false, insertable = false, updatable = false)
  private  Long kerberosKeytabPrincipalId;

  @Id
  @Column(name = "service_name", nullable = false)
  private  String serviceName;

  @Id
  @Column(name = "component_name", nullable = false)
  private  String componentName;

  @ManyToOne
  @JoinColumn(name = "kkp_id")
  private KerberosKeytabPrincipalEntity kerberosKeytabPrincipalEntity;

  public KerberosKeytabServiceMappingEntity() {
  }

  public KerberosKeytabServiceMappingEntity(KerberosKeytabPrincipalEntity kerberosKeytabPrincipalEntity, String serviceName, String componentName) {
    this.kerberosKeytabPrincipalId = kerberosKeytabPrincipalEntity.getKkpId();
    this.kerberosKeytabPrincipalEntity = kerberosKeytabPrincipalEntity;
    this.serviceName = serviceName;
    this.componentName = componentName;
  }

  public Long getKerberosKeytabPrincipalId() {
    return kerberosKeytabPrincipalId;
  }

  public void setKerberosKeytabPrincipalId(Long kerberosKeytabPrincipalId) {
    this.kerberosKeytabPrincipalId = kerberosKeytabPrincipalId;
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

  public KerberosKeytabPrincipalEntity getKerberosKeytabPrincipalEntity() {
    return kerberosKeytabPrincipalEntity;
  }

  public void setKerberosKeytabPrincipalEntity(KerberosKeytabPrincipalEntity kerberosKeytabPrincipalEntity) {
    this.kerberosKeytabPrincipalEntity = kerberosKeytabPrincipalEntity;
  }
}

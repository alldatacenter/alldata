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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Represents entity to hold principal for keytab.
 * Ideally this entity must have natural PK based on ({@link #keytabPath}, {@link #principalName}, {@link #hostId}),
 * but {@link #hostId} in some cases can be null, and also this entity must be used in service mappings(this can
 * cause dup of {@link #keytabPath}, {@link #principalName} fields in related entities), so we have surrogate {@link #kkpId}
 * id and unique constraint on ({@link #keytabPath}, {@link #principalName}, {@link #hostId}).
 */
@Entity
@Table(name = "kerberos_keytab_principal")
@TableGenerator(name = "kkp_id_generator",
  table = "ambari_sequences",
  pkColumnName = "sequence_name",
  valueColumnName = "sequence_value",
  pkColumnValue = "kkp_id_seq"
)
@NamedQueries({
  @NamedQuery(
    name = "KerberosKeytabPrincipalEntity.findAll",
    query = "SELECT kkpe FROM KerberosKeytabPrincipalEntity kkpe"
  ),
  @NamedQuery(
    name = "KerberosKeytabPrincipalEntity.findByHostAndKeytab",
    query = "SELECT kkpe FROM KerberosKeytabPrincipalEntity kkpe WHERE kkpe.hostId=:hostId AND kkpe.keytabPath=:keytabPath"
  ),
  @NamedQuery(
    name = "KerberosKeytabPrincipalEntity.findByPrincipal",
    query = "SELECT kkpe FROM KerberosKeytabPrincipalEntity kkpe WHERE kkpe.principalName=:principalName"
  ),
  @NamedQuery(
    name = "KerberosKeytabPrincipalEntity.findByHost",
    query = "SELECT kkpe FROM KerberosKeytabPrincipalEntity kkpe WHERE kkpe.hostId=:hostId"
  ),
  @NamedQuery(
    name = "KerberosKeytabPrincipalEntity.findByHostKeytabAndPrincipal",
    query = "SELECT kkpe FROM KerberosKeytabPrincipalEntity kkpe WHERE kkpe.hostId=:hostId AND kkpe.keytabPath=:keytabPath AND kkpe.principalName=:principalName"
  ),
  @NamedQuery(
    name = "KerberosKeytabPrincipalEntity.findByKeytabAndPrincipalNullHost",
    query = "SELECT kkpe FROM KerberosKeytabPrincipalEntity kkpe WHERE kkpe.principalName=:principalName AND kkpe.keytabPath=:keytabPath AND kkpe.hostId IS NULL"
  )
})
public class KerberosKeytabPrincipalEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "kkp_id_generator")
  @Column(name = "kkp_id")
  private Long kkpId;

  @Column(name = "keytab_path", updatable = false, nullable = false)
  private String keytabPath;

  @Column(name = "principal_name", updatable = false, nullable = false)
  private String principalName;

  @Column(name = "host_id")
  private Long hostId;

  @Column(name = "is_distributed", nullable = false)
  private Integer isDistributed = 0;

  @ManyToOne
  @JoinColumn(name = "keytab_path", referencedColumnName = "keytab_path", updatable = false, nullable = false, insertable = false)
  private KerberosKeytabEntity kerberosKeytabEntity;

  @ManyToOne
  @JoinColumn(name = "host_id", referencedColumnName = "host_id", updatable = false, insertable = false)
  private HostEntity hostEntity;

  @ManyToOne
  @JoinColumn(name = "principal_name", referencedColumnName = "principal_name", updatable = false, nullable = false, insertable = false)
  private KerberosPrincipalEntity kerberosPrincipalEntity;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "kerberosKeytabPrincipalEntity", orphanRemoval = true)
  private List<KerberosKeytabServiceMappingEntity> serviceMapping = new ArrayList<>();

  public KerberosKeytabPrincipalEntity() {

  }

  public KerberosKeytabPrincipalEntity(KerberosKeytabEntity kerberosKeytabEntity, HostEntity hostEntity, KerberosPrincipalEntity kerberosPrincipalEntity) {
    setKerberosKeytabEntity(kerberosKeytabEntity);
    setHostEntity(hostEntity);
    setKerberosPrincipalEntity(kerberosPrincipalEntity);
  }

  public Long getKkpId() {
    return kkpId;
  }

  public void setKkpId(Long kkpId) {
    this.kkpId = kkpId;
  }

  public Boolean isDistributed() {
    return isDistributed == 1;
  }

  public void setDistributed(Boolean isDistributed) {
    this.isDistributed = isDistributed ? 1 : 0;
  }

  public KerberosKeytabEntity getKerberosKeytabEntity() {
    return kerberosKeytabEntity;
  }

  public void setKerberosKeytabEntity(KerberosKeytabEntity kke) {
    this.kerberosKeytabEntity = kke;
    if (kke != null) {
      keytabPath = kke.getKeytabPath();
    }
  }

  public HostEntity getHostEntity() {
    return hostEntity;
  }

  public void setHostEntity(HostEntity hostEntity) {
    this.hostEntity = hostEntity;
    if (hostEntity != null) {
      hostId = hostEntity.getHostId();
    }
  }

  public KerberosPrincipalEntity getKerberosPrincipalEntity() {
    return kerberosPrincipalEntity;
  }

  public void setKerberosPrincipalEntity(KerberosPrincipalEntity kerberosPrincipalEntity) {
    this.kerberosPrincipalEntity = kerberosPrincipalEntity;
    if (kerberosPrincipalEntity != null) {
      principalName = kerberosPrincipalEntity.getPrincipalName();
    }
  }

  public String getKeytabPath() {
    return kerberosKeytabEntity != null ? kerberosKeytabEntity.getKeytabPath() : null;
  }


  public String getPrincipalName() {
    return kerberosPrincipalEntity != null ? kerberosPrincipalEntity.getPrincipalName() : null;
  }

  public Long getHostId() {
    return hostEntity != null ? hostEntity.getHostId() : null;
  }

  public String getHostName() {
    return hostEntity != null ? hostEntity.getHostName() : null;
  }

  public List<KerberosKeytabServiceMappingEntity> getServiceMapping() {
    return serviceMapping;
  }

  public void setServiceMapping(List<KerberosKeytabServiceMappingEntity> serviceMapping) {
    this.serviceMapping = (serviceMapping == null)
        ? Collections.emptyList()
        : new ArrayList<>(serviceMapping);
  }

  public boolean putServiceMapping(String service, String component) {
    if (containsMapping(service, component)) {
      return false;
    } else {
      serviceMapping.add(new KerberosKeytabServiceMappingEntity(this, service, component));
      return true;
    }
  }

  public Multimap<String, String> getServiceMappingAsMultimap() {
    Multimap<String, String> result = ArrayListMultimap.create();
    for (KerberosKeytabServiceMappingEntity mappingEntity : serviceMapping) {
      result.put(mappingEntity.getServiceName(), mappingEntity.getComponentName());
    }
    return result;
  }

  public boolean containsMapping(String serviceName, String componentName) {
    for (KerberosKeytabServiceMappingEntity mappingEntity : serviceMapping) {
      if (Objects.equal(mappingEntity.getComponentName(), componentName)
        && Objects.equal(mappingEntity.getServiceName(), serviceName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    KerberosKeytabPrincipalEntity that = (KerberosKeytabPrincipalEntity) o;
    return Objects.equal(keytabPath, that.keytabPath) &&
      Objects.equal(principalName, that.principalName) &&
      Objects.equal(hostId, that.hostId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(keytabPath, principalName, hostId);
  }
}

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.orm.entities;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The {@link MpackEntity} class represents the mpack objects in the cluster.
 */

@Table(name = "mpacks")
@Entity
@TableGenerator(name = "mpack_id_generator", table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value", pkColumnValue = "mpack_id_seq", initialValue = 1)
@NamedQueries({
        @NamedQuery(name = "MpackEntity.findById", query = "SELECT mpack FROM MpackEntity mpack where mpack.id = :id"),
        @NamedQuery(name = "MpackEntity.findAll", query = "SELECT mpack FROM MpackEntity mpack"),
        @NamedQuery(name = "MpackEntity.findByNameVersion", query = "SELECT mpack FROM MpackEntity mpack where mpack.mpackName = :mpackName and mpack.mpackVersion = :mpackVersion")})

public class MpackEntity {
  protected final static Logger LOG = LoggerFactory.getLogger(MpackEntity.class);
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "mpack_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "registry_id", nullable = true, insertable = true, updatable = false, length = 10)
  private Long registryId;

  @Column(name = "mpack_name", nullable = false, updatable = true)
  private String mpackName;

  @Column(name = "mpack_version", nullable = false)
  private String mpackVersion;

  @Column(name = "mpack_uri", nullable = false)
  private String mpackUri;

  public Long getId() {
    return id;
  }

  public Long getRegistryId() {
    return registryId;
  }

  public String getMpackName() {
    return mpackName;
  }

  public String getMpackVersion() {
    return mpackVersion;
  }

  public String getMpackUri() {
    return mpackUri;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setRegistryId(Long registryId) {
    this.registryId = registryId;
  }

  public void setMpackName(String mpackName) {
    this.mpackName = mpackName;
  }

  public void setMpackVersion(String mpackVersion) {
    this.mpackVersion = mpackVersion;
  }

  public void setMpackUri(String mpackUri) {
    this.mpackUri = mpackUri;
  }

  public MpackEntity() {

  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    MpackEntity that = (MpackEntity) object;
    EqualsBuilder equalsBuilder = new EqualsBuilder();

    equalsBuilder.append(id, that.id);
    equalsBuilder.append(mpackName, that.mpackName);
    equalsBuilder.append(mpackVersion, that.mpackVersion);
    return equalsBuilder.isEquals();
  }

  /**
   * Generates a hash for the mpack based on the following criteria:
   * <ul>
   * <li>{@link #id}
   * </ul>
   * <p/>
   * <p/>
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(id, mpackName, mpackVersion);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("MpackEntity{");
    buffer.append("id=").append(id);
    if (null != registryId) {
      buffer.append(", registryId=").append(registryId);
    }
    buffer.append(", mpackName=").append(mpackName);
    buffer.append(", mpackVersion=").append(mpackVersion);
    buffer.append(", mpackUri=").append(mpackUri);
    buffer.append("}");
    return buffer.toString();
  }
}


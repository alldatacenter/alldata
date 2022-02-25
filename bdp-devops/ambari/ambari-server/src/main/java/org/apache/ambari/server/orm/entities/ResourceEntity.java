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

import java.util.Collection;
import java.util.HashSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * Represents a resource.
 */
@Table(name = "adminresource")
@Entity
@TableGenerator(name = "resource_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "resource_id_seq"
    , initialValue = 2
)
public class ResourceEntity {

  /**
   * The Ambari admin resource ID.
   */
  public final static long AMBARI_RESOURCE_ID = 1L;

  /**
   * The type id.
   */
  @Id
  @Column(name = "resource_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "resource_id_generator")
  private Long id;

  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "resource_type_id", referencedColumnName = "resource_type_id", nullable = false)
  })
  private ResourceTypeEntity resourceType;

  /**
   * The list of privileges.
   */
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "resource")
  private Collection<PrivilegeEntity> privileges = new HashSet<>();


  // ----- ResourceEntity ---------------------------------------------------

  /**
   * Get the resource type id.
   *
   * @return the resource type id.
   */
  public Long getId() {
    return id;
  }

  /**
   * Set the resource id.
   *
   * @param id  the type id.
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Get the resource type entity.
   *
   * @return  the resource type entity
   */
  public ResourceTypeEntity getResourceType() {
    return resourceType;
  }

  /**
   * Set the resource type entity.
   *
   * @param resourceType  the resource type entity
   */
  public void setResourceType(ResourceTypeEntity resourceType) {
    this.resourceType = resourceType;
  }

  /**
   * Get the associated privileges.
   *
   * @return the privileges
   */
  public Collection<PrivilegeEntity> getPrivileges() {
    return privileges;
  }

  /**
   * Set the associated privileges.
   *
   * @param privileges the privileges
   */
  public void setPrivileges(Collection<PrivilegeEntity> privileges) {
    this.privileges = privileges;
  }


  // ----- Object overrides --------------------------------------------------


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ResourceEntity that = (ResourceEntity) o;

    return !(id != null ? !id.equals(that.id) : that.id != null) && !(resourceType != null ?
        !resourceType.equals(that.resourceType) : that.resourceType != null);

  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (resourceType != null ? resourceType.hashCode() : 0);
    return result;
  }
}

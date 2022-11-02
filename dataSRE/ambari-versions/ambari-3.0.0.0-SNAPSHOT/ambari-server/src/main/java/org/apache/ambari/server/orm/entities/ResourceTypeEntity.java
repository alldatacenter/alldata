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
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * Represents a resource type.
 */
@Table(name = "adminresourcetype")
@Entity
@TableGenerator(name = "resource_type_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "resource_type_id_seq"
    , initialValue = 4
)
public class ResourceTypeEntity {

  /**
   * The type id.
   */
  @Id
  @Column(name = "resource_type_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "resource_type_id_generator")
  private Integer id;

  /**
   * The type name.
   */
  @Column(name = "resource_type_name")
  private String name;


  // ----- ResourceTypeEntity -----------------------------------------------

  /**
   * Get the resource type id.
   *
   * @return the resource type id.
   */
  public Integer getId() {
    return id;
  }

  /**
   * Set the resource type id.
   *
   * @param id  the type id.
   */
  public void setId(Integer id) {
    this.id = id;
  }

  /**
   * Get the resource type name.
   *
   * @return the resource type name;
   */
  public String getName() {
    return name;
  }

  /**
   * Set the resource type name.
   *
   * @param name  the resource type name.
   */
  public void setName(String name) {
    this.name = name;
  }


  // ----- Object overrides --------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ResourceTypeEntity that = (ResourceTypeEntity) o;

    return !(id != null ? !id.equals(that.id) : that.id != null) && !(name != null ?
        !name.equals(that.name) : that.name != null);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ResourceTypeEntity [id=" + id + ", name=" + name + "]";
  }

}


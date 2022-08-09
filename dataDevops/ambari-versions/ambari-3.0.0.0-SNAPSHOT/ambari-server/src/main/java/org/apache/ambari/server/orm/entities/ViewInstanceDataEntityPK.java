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
import javax.persistence.Id;

/**
 * Composite primary key for ViewInstanceDataEntity.
 */
public class ViewInstanceDataEntityPK {

  @Id
  @Column(name = "view_instance_id", nullable = false, insertable = false, updatable = false)
  private Long viewInstanceId;

  @Id
  @Column(name = "name", nullable = false, insertable = true, updatable = false, length = 100)
  private String name;

  @Id
  @Column(name = "user_name", nullable = false, insertable = true, updatable = false, length = 100)
  private String user;

  public Long getViewInstanceId() {
    return viewInstanceId;
  }

  public void setViewInstanceId(Long viewInstanceId) {
    this.viewInstanceId = viewInstanceId;
  }

  /**
   * Get the name of the data entry.
   *
   * @return host group name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of the data entry.
   *
   * @param name  host group name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the associated user.
   *
   * @return the user
   */
  public String getUser() {
    return user;
  }

  /**
   * Set the user.
   *
   * @param user  the user
   */
  public void setUser(String user) {
    this.user = user;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ViewInstanceDataEntityPK that = (ViewInstanceDataEntityPK) o;

    return name.equals(that.name) && user.equals(that.user) && viewInstanceId.equals(that.viewInstanceId);

  }

  @Override
  public int hashCode() {
    int result = viewInstanceId.hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + user.hashCode();
    return result;
  }
}

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

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Composite primary key for {@link AmbariConfigurationEntity}.
 */
public class AmbariConfigurationEntityPK implements Serializable {

  private String categoryName;
  private String propertyName;

  /**
   * Constructor.
   *
   * @param categoryName configuration category name
   * @param propertyName configuration property name
   */
  public AmbariConfigurationEntityPK(String categoryName, String propertyName) {
    this.categoryName = categoryName;
    this.propertyName = propertyName;
  }

  /**
   * Get the configuration category name.
   *
   * @return category name
   */
  public String getCategoryName() {
    return categoryName;
  }

  /**
   * Get the property name.
   *
   * @return property name
   */
  public String getPropertyName() {
    return propertyName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AmbariConfigurationEntityPK that = (AmbariConfigurationEntityPK) o;

    return new EqualsBuilder()
        .append(categoryName, that.categoryName)
        .append(propertyName, that.propertyName)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(categoryName)
        .append(propertyName)
        .toHashCode();
  }
}

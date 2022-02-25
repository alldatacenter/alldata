/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

@Table(name = "ambari_configuration")
@NamedQueries({
    @NamedQuery(
        name = "AmbariConfigurationEntity.findByCategory",
        query = "select ace from AmbariConfigurationEntity ace where ace.categoryName = :categoryName"),
    @NamedQuery(
        name = "AmbariConfigurationEntity.deleteByCategory",
        query = "delete from AmbariConfigurationEntity ace where ace.categoryName = :categoryName")
})
@IdClass(AmbariConfigurationEntityPK.class)
@Entity
public class AmbariConfigurationEntity {

  @Id
  @Column(name = "category_name")
  private String categoryName;

  @Id
  @Column(name = "property_name")
  private String propertyName;

  @Column(name = "property_value")
  private String propertyValue;

  public String getCategoryName() {
    return categoryName;
  }

  public void setCategoryName(String category) {
    this.categoryName = category;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public void setPropertyName(String propertyName) {
    this.propertyName = propertyName;
  }

  public String getPropertyValue() {
    return propertyValue;
  }

  public void setPropertyValue(String propertyValue) {
    this.propertyValue = propertyValue;
  }

  @Override
  public String toString() {
    return "AmbariConfigurationEntity{" +
        " category=" + categoryName +
        ", name=" + propertyName +
        ", value=" + propertyValue +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AmbariConfigurationEntity that = (AmbariConfigurationEntity) o;

    return new EqualsBuilder()
        .append(categoryName, that.categoryName)
        .append(propertyName, that.propertyName)
        .append(propertyValue, that.propertyValue)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(categoryName)
        .append(propertyName)
        .append(propertyValue)
        .toHashCode();
  }
}

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
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Table(name = "metainfo")
@Entity
public class MetainfoEntity {

  @Column(name = "\"metainfo_key\"", length = 255)
  @Id
  private String metainfoName;

  @Column(name = "\"metainfo_value\"", length = 32000)
  @Basic
  @Lob
  private String metainfoValue;

  public String getMetainfoName() {
    return metainfoName;
  }

  public void setMetainfoName(String metainfoName) {
    this.metainfoName = metainfoName;
  }

  public String getMetainfoValue() {
    return metainfoValue;
  }

  public void setMetainfoValue(String metainfoValue) {
    this.metainfoValue = metainfoValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MetainfoEntity that = (MetainfoEntity) o;

    if (metainfoName != null ? !metainfoName.equals(that.metainfoName) : that.metainfoName != null) {
      return false;
    }
    if (metainfoValue != null ? !metainfoValue.equals(that.metainfoValue) : that.metainfoValue != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = metainfoName != null ? metainfoName.hashCode() : 0;
    result = 31 * result + (metainfoValue != null ? metainfoValue.hashCode() : 0);
    return result;
  }
}

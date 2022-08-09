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

import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * Entity representing Setting.
 */
@Table(name = "setting")
@TableGenerator(name = "setting_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value",
        pkColumnValue = "setting_id_seq", initialValue = 0)

@NamedQuery(name = "settingByName", query = "SELECT setting FROM SettingEntity setting WHERE setting.name=:name")
@Entity
public class SettingEntity {
  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "setting_id_generator")
  private long id;

  @Column(name = "name", nullable = false, insertable = true, updatable = false, unique = true)
  @Basic
  private String name;

  @Column(name = "setting_type", nullable = false, insertable = true, updatable = true)
  @Basic
  private String settingType;

  @Column(name = "content", nullable = false, insertable = true, updatable = true)
  @Lob
  @Basic
  private String content;

  @Column(name = "updated_by", nullable = false, insertable = true, updatable = true)
  @Basic
  private String updatedBy;

  @Column(name = "update_timestamp", nullable = false, insertable = true, updatable = true)
  @Basic
  private long updateTimestamp;

  public SettingEntity() {
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSettingType() {
    return settingType;
  }

  public void setSettingType(String settingType) {
    this.settingType = settingType;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public long getUpdateTimestamp() {
    return updateTimestamp;
  }

  public void setUpdateTimestamp(long updateTimestamp) {
    this.updateTimestamp = updateTimestamp;
  }

  @Override
  public SettingEntity clone() {
    SettingEntity cloned = new SettingEntity();
    cloned.setId(id);
    cloned.setName(name);
    cloned.setContent(content);
    cloned.setSettingType(settingType);
    cloned.setUpdatedBy(updatedBy);
    cloned.setUpdateTimestamp(updateTimestamp);
    return cloned;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SettingEntity entity = (SettingEntity) o;
    return id == entity.id &&
            Objects.equals(name, entity.name) &&
            Objects.equals(settingType, entity.settingType) &&
            Objects.equals(content, entity.content) &&
            Objects.equals(updatedBy, entity.updatedBy) &&
            updateTimestamp == entity.updateTimestamp;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, settingType, content, updatedBy, updateTimestamp);
  }
}

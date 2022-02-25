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
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

/**
 * Represents a blueprint setting.
 */
@Table(name = "blueprint_setting", uniqueConstraints =
@UniqueConstraint(
        name = "UQ_blueprint_setting_name", columnNames = {"blueprint_name", "setting_name"}
  )
)

@TableGenerator(name = "blueprint_setting_id_generator",
        table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value",
        pkColumnValue = "blueprint_setting_id_seq", initialValue = 0)

@Entity
public class BlueprintSettingEntity {

  @Id
  @Column(name = "id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "blueprint_setting_id_generator")
  private long id;

  @Column(name = "blueprint_name", nullable = false, insertable = false, updatable = false)
  private String blueprintName;

  @Column(name = "setting_name", nullable = false, insertable = true, updatable = false)
  private String settingName;

  @Column(name = "setting_data", nullable = false, insertable = true, updatable = false)
  @Basic(fetch = FetchType.LAZY)
  @Lob
  private String settingData;

  @ManyToOne
  @JoinColumn(name = "blueprint_name", referencedColumnName = "blueprint_name", nullable = false)
  private BlueprintEntity blueprint;

  /**
   * Get the blueprint entity instance.
   *
   * @return blueprint entity
   */
  public BlueprintEntity getBlueprintEntity() {
    return blueprint;
  }

  /**
   * Set the blueprint entity instance.
   *
   * @param entity  blueprint entity
   */
  public void setBlueprintEntity(BlueprintEntity entity) {
    this.blueprint = entity;
  }

  /**
   * Get the name of the associated blueprint.
   *
   * @return blueprint name
   */
  public String getBlueprintName() {
    return blueprintName;
  }

  /**
   * Set the name of the associated blueprint.
   * '
   * @param blueprintName  blueprint name
   */
  public void setBlueprintName(String blueprintName) {
    this.blueprintName = blueprintName;
  }

  /**
   * Get the setting name.
   *
   * @return setting name
   */
  public String getSettingName() {
    return settingName;
  }

  /**
   * Set the setting name.
   *
   * @param settingName  setting name
   */
  public void setSettingName(String settingName) {
    this.settingName = settingName;
  }

  /**
   * Get the setting data.
   *
   * @return setting data in json format
   */
  public String getSettingData() {
    return settingData;
  }

  /**
   * Set the setting data.
   *
   * @param settingData  all config data in json format
   */
  public void setSettingData(String settingData) {
    this.settingData = settingData;
  }
}

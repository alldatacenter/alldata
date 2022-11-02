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

import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * Models a single upgrade group as part of an entire {@link UpgradeEntity}.
 * <p/>
 * Since {@link UpgradeGroupEntity} instances are rarely created, yet created in
 * bulk, we have an abnormally high {@code allocationSize}} for the
 * {@link TableGenerator}. This helps prevent locks caused by frequenty queries
 * to the sequence ID table.
 */
@Table(name = "upgrade_group")
@Entity
@TableGenerator(name = "upgrade_group_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value",
    pkColumnValue = "upgrade_group_id_seq",
    initialValue = 0,
    allocationSize = 200)
public class UpgradeGroupEntity {

  @Id
  @Column(name = "upgrade_group_id", nullable = false, insertable = true, updatable = false)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "upgrade_group_id_generator")
  private Long upgradeGroupId;

  @Column(name = "upgrade_id", nullable = false, insertable = false, updatable = false)
  private Long upgradeId;

  @Basic
  @Column(name = "group_name", length=255, nullable = false)
  private String groupName;

  @Basic
  @Column(name = "group_title", length=1024, nullable = false)
  private String groupTitle;


  @ManyToOne
  @JoinColumn(name = "upgrade_id", referencedColumnName = "upgrade_id", nullable = false)
  private UpgradeEntity upgradeEntity;

  @OneToMany(mappedBy ="upgradeGroupEntity", cascade = { CascadeType.ALL })
  private List<UpgradeItemEntity> upgradeItems;


  /**
   * @return the id
   */
  public Long getId() {
    return upgradeGroupId;
  }

  /**
   * @param id the id
   */
  public void setId(Long id) {
    upgradeGroupId = id;
  }

  /**
   * @return the group name
   */
  public String getName() {
    return groupName;
  }

  /**
   * @param name the name
   */
  public void setName(String name) {
    groupName = name;
  }


  /**
   * @return the item text
   */
  public String getTitle() {
    return groupTitle;
  }

  /**
   * @param title the item text
   */
  public void setTitle(String title) {
    groupTitle = title;
  }

  public UpgradeEntity getUpgradeEntity() {
    return upgradeEntity;
  }

  public void setUpgradeEntity(UpgradeEntity entity) {
    upgradeEntity = entity;
  }

  public List<UpgradeItemEntity> getItems() {
    return upgradeItems;
  }

  public void setItems(List<UpgradeItemEntity> items) {
    for (UpgradeItemEntity item : items) {
      item.setGroupEntity(this);
    }
    upgradeItems = items;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("UpgradeGroupEntity{");
    buffer.append("upgradeGroupId=").append(upgradeGroupId);
    buffer.append(", upgradeId=").append(upgradeId);
    buffer.append(", groupName=").append(groupName);
    buffer.append(", groupTitle=").append(groupTitle);
    buffer.append("}");
    return buffer.toString();
  }

}

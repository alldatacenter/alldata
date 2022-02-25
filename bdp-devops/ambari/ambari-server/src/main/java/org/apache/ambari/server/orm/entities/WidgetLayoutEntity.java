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
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "widget_layout")
@TableGenerator(name = "widget_layout_id_generator",
        table = "ambari_sequences",
        pkColumnName = "sequence_name",
        valueColumnName = "sequence_value",
        pkColumnValue = "widget_layout_id_seq",
        initialValue = 0,
        uniqueConstraints=@UniqueConstraint(columnNames={"layout_name", "cluster_id"})
)
@NamedQueries({
    @NamedQuery(name = "WidgetLayoutEntity.findAll", query = "SELECT widgetLayout FROM WidgetLayoutEntity widgetLayout"),
    @NamedQuery(name = "WidgetLayoutEntity.findByCluster", query = "SELECT widgetLayout FROM WidgetLayoutEntity widgetLayout WHERE widgetLayout.clusterId = :clusterId"),
    @NamedQuery(name = "WidgetLayoutEntity.findBySectionName", query = "SELECT widgetLayout FROM WidgetLayoutEntity widgetLayout WHERE widgetLayout.sectionName = :sectionName"),
    @NamedQuery(name = "WidgetLayoutEntity.findByName", query = "SELECT widgetLayout FROM WidgetLayoutEntity widgetLayout WHERE widgetLayout.clusterId = :clusterId AND widgetLayout.layoutName = :layoutName AND widgetLayout.userName = :userName")
    })
public class WidgetLayoutEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "widget_layout_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "layout_name", nullable = false, length = 255)
  private String layoutName;

  @Column(name = "section_name", nullable = false, length = 255)
  private String sectionName;

  @Column(name = "cluster_id", nullable = false)
  private Long clusterId;

  @Column(name = "user_name", nullable = false)
  private String userName;

  @Column(name = "scope", nullable = false)
  private String scope;

  @Column(name = "display_name")
  private String displayName;

  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false, updatable = false, insertable = false)
  private ClusterEntity clusterEntity;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "widgetLayout", orphanRemoval = true)
  @OrderBy("widgetOrder")
  private List<WidgetLayoutUserWidgetEntity> listWidgetLayoutUserWidgetEntity;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getLayoutName() {
    return layoutName;
  }

  public void setLayoutName(String layoutName) {
    this.layoutName = layoutName;
  }

  public String getSectionName() {
    return sectionName;
  }

  public void setSectionName(String sectionName) {
    this.sectionName = sectionName;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  public List<WidgetLayoutUserWidgetEntity> getListWidgetLayoutUserWidgetEntity() {
    return listWidgetLayoutUserWidgetEntity;
  }

  public void setListWidgetLayoutUserWidgetEntity(List<WidgetLayoutUserWidgetEntity> listWidgetLayoutUserWidgetEntity) {
    this.listWidgetLayoutUserWidgetEntity = listWidgetLayoutUserWidgetEntity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WidgetLayoutEntity that = (WidgetLayoutEntity) o;

    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

}

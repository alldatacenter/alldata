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

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "widget")
@TableGenerator(name = "widget_id_generator",
        table = "ambari_sequences",
        pkColumnName = "sequence_name",
        valueColumnName = "sequence_value",
        pkColumnValue = "widget_id_seq",
        initialValue = 0
)
@NamedQueries({
    @NamedQuery(name = "WidgetEntity.findAll", query = "SELECT widget FROM WidgetEntity widget"),
    @NamedQuery(name = "WidgetEntity.findByScopeOrAuthor", query =
            "SELECT widget FROM WidgetEntity widget " +
                    "WHERE widget.author = :author " +
                    "OR widget.scope = :scope"),
    @NamedQuery(name = "WidgetEntity.findByCluster", query = "SELECT widget FROM WidgetEntity widget WHERE widget.clusterId = :clusterId"),
    @NamedQuery(name = "WidgetEntity.findByName", query =
            "SELECT widget FROM WidgetEntity widget " +
                    "WHERE widget.clusterId = :clusterId " +
                    "AND widget.widgetName = :widgetName " +
                    "AND widget.author = :author " +
                    "AND widget.defaultSectionName = :defaultSectionName"),
    @NamedQuery(name = "WidgetEntity.findBySectionName", query =
                "SELECT widget FROM WidgetEntity widget " +
                "INNER JOIN widget.listWidgetLayoutUserWidgetEntity widgetLayoutUserWidget " +
                "INNER JOIN widgetLayoutUserWidget.widgetLayout  widgetLayout " +
                "WHERE widgetLayout.sectionName = :sectionName")
        })
public class WidgetEntity {

  public static final String CLUSTER_SCOPE = "CLUSTER";
  public static final String USER_SCOPE = "USER";

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "widget_id_generator")
  @Column(name = "id", nullable = false, updatable = false)
  private Long id;

  @Column(name = "widget_name", nullable = false, length = 255)
  private String widgetName;

  @Column(name = "widget_type", nullable = false, length = 255)
  private String widgetType;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "metrics")
  private String metrics;

  @Column(name = "time_created", nullable = false, length = 255)
  private Long timeCreated = System.currentTimeMillis();

  @Column(name = "author", length = 255)
  private String author;

  @Column(name = "description", length = 255)
  private String description;

  @Column(name = "default_section_name",  length = 255, nullable = true)
  private String defaultSectionName;

  @Column(name = "scope", length = 255)
  private String scope;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "widget_values")
  private String widgetValues;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "properties")
  private String properties;

  @Column(name = "cluster_id", nullable = false)
  private Long clusterId;

  @Column(name = "tag", length = 255)
  private String tag;

  @ManyToOne
  @JoinColumn(name = "cluster_id", referencedColumnName = "cluster_id", nullable = false, updatable = false, insertable = false)
  private ClusterEntity clusterEntity;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "widget", orphanRemoval = true)
  private List<WidgetLayoutUserWidgetEntity> listWidgetLayoutUserWidgetEntity;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getWidgetName() {
    return widgetName;
  }

  public void setWidgetName(String widgetName) {
    this.widgetName = widgetName;
  }

  public String getWidgetType() {
    return widgetType;
  }

  public void setWidgetType(String widgetType) {
    this.widgetType = widgetType;
  }

  public String getMetrics() {
    return metrics;
  }

  public void setMetrics(String metrics) {
    this.metrics = metrics;
  }

  public Long getTimeCreated() {
    return timeCreated;
  }

  public void setTimeCreated(Long timeCreated) {
    this.timeCreated = timeCreated;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDefaultSectionName() {
    return defaultSectionName;
  }

  public void setDefaultSectionName(String displayName) {
    this.defaultSectionName = displayName;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getWidgetValues() {
    return widgetValues;
  }

  public void setWidgetValues(String widgetValues) {
    this.widgetValues = widgetValues;
  }

  public String getProperties() {
    return properties;
  }

  public void setProperties(String properties) {
    this.properties = properties;
  }

  public ClusterEntity getClusterEntity() {
    return clusterEntity;
  }

  public void setClusterEntity(ClusterEntity clusterEntity) {
    this.clusterEntity = clusterEntity;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  /**
   * Gets the Tag used by the ui to store additional info e.g.:Name Service
   */
  public String getTag() {
    return tag;
  }

  /**
   * Sets the Tag used by the ui to store additional info e.g.:Name Service
   */
  public void setTag(String tag) {
    this.tag = tag;
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

    WidgetEntity that = (WidgetEntity) o;

    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

}

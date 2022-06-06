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
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * Represents an entity of a View.
 */
@Table(name = "viewurl")
@TableGenerator(name = "viewurl_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "viewurl_id_seq"
    , initialValue = 1
)

@NamedQueries({
        @NamedQuery(name = "allViewUrls", query = "SELECT viewUrl FROM ViewURLEntity viewUrl"),
        @NamedQuery(name = "viewUrlByName", query ="SELECT viewUrlEntity FROM ViewURLEntity viewUrlEntity WHERE viewUrlEntity.urlName=:urlName"),
        @NamedQuery(name = "viewUrlBySuffix", query ="SELECT viewUrlEntity FROM ViewURLEntity viewUrlEntity WHERE viewUrlEntity.urlSuffix=:urlSuffix")})

@Entity
public class ViewURLEntity {

  @Column(name = "url_id")
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "viewentity_id_generator")
  private Long id;

  /**
   * The view name.
   */
  @Column(name = "url_name", nullable = false, insertable = true, updatable = false)
  private String urlName;

  @Column(name = "url_suffix", nullable = false, insertable = true, updatable = true)
  private String urlSuffix;

  @OneToOne(fetch= FetchType.LAZY, mappedBy="viewUrl")
  private ViewInstanceEntity viewInstanceEntity;


  /**
   * Get the URL suffix
   * @return URL suffix
     */
  public String getUrlSuffix() {
    return urlSuffix;
  }

  /**
   * Set the URL suffix
     */
  public void setUrlSuffix(String urlSuffix) {
    this.urlSuffix = urlSuffix;
  }


  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Get the URL name.
   *
   * @return the URL name
   */
  public String getUrlName() {
    return urlName;
  }

  /**
   * Set the URL name
   *
   * @param urlName  the URL name
   */
  public void setUrlName(String urlName) {
    this.urlName = urlName;
  }

  /**
   *  Get the linked instance entity
   * @return viewInstanceEntity
     */
  public ViewInstanceEntity getViewInstanceEntity() {
    return viewInstanceEntity;
  }

  /**
   * Set the URL instance entity
   * @param viewInstanceEntity
     */
  public void setViewInstanceEntity(ViewInstanceEntity viewInstanceEntity) {
    this.viewInstanceEntity = viewInstanceEntity;
  }

  /**
   * Remove the Instance entity associated with this View URL
   */
  public void clearEntity() {
    this.viewInstanceEntity = null;
  }

}

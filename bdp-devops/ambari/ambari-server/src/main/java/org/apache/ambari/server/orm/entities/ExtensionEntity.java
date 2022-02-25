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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

/**
 * The {@link ExtensionEntity} class is used to model an extension to the stack.
 *
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
@Entity
@Table(name = "extension", uniqueConstraints = @UniqueConstraint(columnNames = {
    "extension_name", "extension_version" }))
@TableGenerator(name = "extension_id_generator", table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value", pkColumnValue = "extension_id_seq", initialValue = 0)
@NamedQueries({
    @NamedQuery(name = "ExtensionEntity.findAll", query = "SELECT extension FROM ExtensionEntity extension"),
    @NamedQuery(name = "ExtensionEntity.findByNameAndVersion", query = "SELECT extension FROM ExtensionEntity extension WHERE extension.extensionName = :extensionName AND extension.extensionVersion = :extensionVersion") })
public class ExtensionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "extension_id_generator")
  @Column(name = "extension_id", nullable = false, updatable = false)
  private Long extensionId;

  @Column(name = "extension_name", length = 255, nullable = false)
  private String extensionName;

  @Column(name = "extension_version", length = 255, nullable = false)
  private String extensionVersion;

  /**
   * Constructor.
   */
  public ExtensionEntity() {
  }

  /**
   * Gets the unique identifier for this extension.
   *
   * @return the ID.
   */
  public Long getExtensionId() {
    return extensionId;
  }

  /**
   * Gets the name of the extension.
   *
   * @return the name of the extension (never {@code null}).
   */
  public String getExtensionName() {
    return extensionName;
  }

  /**
   * Sets the name of the extension.
   *
   * @param extensionName
   *          the extension name (not {@code null}).
   */
  public void setExtensionName(String extensionName) {
    this.extensionName = extensionName;
  }

  /**
   * Gets the version of the extension.
   *
   * @return the extension version (never {@code null}).
   */
  public String getExtensionVersion() {
    return extensionVersion;
  }

  /**
   * Sets the version of the extension.
   *
   * @param extensionVersion
   *          the extension version (not {@code null}).
   */
  public void setExtensionVersion(String extensionVersion) {
    this.extensionVersion = extensionVersion;
  }

  /**
   *
   */
  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    ExtensionEntity that = (ExtensionEntity) object;

    if (extensionId != null ? !extensionId.equals(that.extensionId) : that.extensionId != null) {
      return false;
    }

    return true;
  }

  /**
   *
   */
  @Override
  public int hashCode() {
    int result = null != extensionId ? extensionId.hashCode() : 0;
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(getClass().getSimpleName());
    buffer.append("{");
    buffer.append("id=").append(extensionId);
    buffer.append(", name=").append(extensionName);
    buffer.append(", version=").append(extensionVersion);
    buffer.append("}");
    return buffer.toString();
  }
}

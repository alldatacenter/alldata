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
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;

/**
 * The {@link ExtensionLinkEntity} class is used to model the extensions linked to the stack.
 *
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
@Table(name = "extensionlink", uniqueConstraints = @UniqueConstraint(columnNames = {
		"stack_id", "extension_id" }))
@TableGenerator(name = "link_id_generator", table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value", pkColumnValue = "link_id_seq", initialValue = 0)
@NamedQueries({
    @NamedQuery(name = "ExtensionLinkEntity.findAll", query = "SELECT link FROM ExtensionLinkEntity link"),
    @NamedQuery(name = "ExtensionLinkEntity.findByStackAndExtensionName", query = "SELECT link FROM ExtensionLinkEntity link WHERE link.stack.stackName = :stackName AND link.stack.stackVersion = :stackVersion AND link.extension.extensionName = :extensionName"),
    @NamedQuery(name = "ExtensionLinkEntity.findByStackAndExtension", query = "SELECT link FROM ExtensionLinkEntity link WHERE link.stack.stackName = :stackName AND link.stack.stackVersion = :stackVersion AND link.extension.extensionName = :extensionName AND link.extension.extensionVersion = :extensionVersion"),
    @NamedQuery(name = "ExtensionLinkEntity.findByStack", query = "SELECT link FROM ExtensionLinkEntity link WHERE link.stack.stackName = :stackName AND link.stack.stackVersion = :stackVersion"),
    @NamedQuery(name = "ExtensionLinkEntity.findByExtension", query = "SELECT link FROM ExtensionLinkEntity link WHERE link.extension.extensionName = :extensionName AND link.extension.extensionVersion = :extensionVersion") })
@Entity
public class ExtensionLinkEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "link_id_generator")
  @Column(name = "link_id", nullable = false, updatable = false)
  private Long linkId;

  @OneToOne
  @JoinColumn(name = "stack_id", unique = false, nullable = false, insertable = true, updatable = false)
  private StackEntity stack;

  @OneToOne
  @JoinColumn(name = "extension_id", unique = false, nullable = false, insertable = true, updatable = false)
  private ExtensionEntity extension;

  /**
   * Constructor.
   */
  public ExtensionLinkEntity() {
  }

  public Long getLinkId() {
    return linkId;
  }

  public void setLinkId(Long linkId) {
    this.linkId = linkId;
  }

  public StackEntity getStack() {
    return stack;
  }

  public void setStack(StackEntity stack) {
    this.stack = stack;
  }

  public ExtensionEntity getExtension() {
    return extension;
  }

  public void setExtension(ExtensionEntity extension) {
    this.extension = extension;
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

    ExtensionLinkEntity that = (ExtensionLinkEntity) object;

    if (linkId != null ? !linkId.equals(that.linkId) : that.linkId != null) {
      return false;
    }

    return true;
  }

  /**
   *
   */
  @Override
  public int hashCode() {
    int result = (null != linkId) ? linkId.hashCode() : 0;
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
    buffer.append("linkId=").append(linkId);
    buffer.append(", stackId=").append(stack.getStackId());
    buffer.append(", extensionId=").append(extension.getExtensionId());
    buffer.append("}");
    return buffer.toString();
  }
}

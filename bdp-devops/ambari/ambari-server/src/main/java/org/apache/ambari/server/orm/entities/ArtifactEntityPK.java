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
import javax.persistence.Id;

/**
 * Composite primary key for ArtifactEntity.
 */
public class ArtifactEntityPK {
  @Id
  @Column(name = "artifact_name", nullable = false, insertable = true, updatable = false)
  private String artifactName;

  @Id
  @Column(name = "foreign_keys", nullable = false, insertable = true, updatable = false)
  private String foreignKeys;

  /**
   * Constructor.
   *
   @param artifactName  artifact name
   @param foreignKeys   foreign key information
   */
  public ArtifactEntityPK(String artifactName, String foreignKeys) {
    this.artifactName = artifactName;
    this.foreignKeys = foreignKeys;
  }

  /**
   * Get the name of the associated artifact.
   *
   * @return artifact name
   */
  public String getArtifactName() {
    return artifactName;
  }

  /**
   * Set the name of the associated artifact.
   *
   * @param name  artifact name
   */
  public void setArtifactName(String name) {
    artifactName = name;
  }

  /**
   * Get the foreign key information of the associated artifact.
   *
   * @return foreign key information
   */
  public String getForeignKeys() {
    return foreignKeys;
  }

  /**
   * Set the foreign key information of the associated artifact.
   *
   * @param foreignKeys  foreign key information
   */
  public void setForeignKeys(String foreignKeys) {
    this.foreignKeys = foreignKeys;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArtifactEntityPK that = (ArtifactEntityPK) o;

    return this.artifactName.equals(that.artifactName) &&
        this.foreignKeys.equals(that.foreignKeys);
  }

  @Override
  public int hashCode() {
    return 31 * artifactName.hashCode() + foreignKeys.hashCode();
  }
}

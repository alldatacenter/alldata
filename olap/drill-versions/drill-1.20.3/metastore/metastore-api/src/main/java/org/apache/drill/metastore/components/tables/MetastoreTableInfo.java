/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.metastore.components.tables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.metastore.metadata.TableInfo;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Holds metastore table metadata information, including table information, exists status,
 * last modified time and metastore version.
 */
public class MetastoreTableInfo {

  private final TableInfo tableInfo;
  private final Long lastModifiedTime;
  private final boolean exists;
  private final long metastoreVersion;

  @JsonCreator
  public MetastoreTableInfo(@JsonProperty("tableInfo") TableInfo tableInfo,
      @JsonProperty("lastModifiedTime") Long lastModifiedTime,
      @JsonProperty("exists") boolean exists,
      @JsonProperty("metastoreVersion") long metastoreVersion) {
    this.tableInfo = tableInfo;
    this.lastModifiedTime = lastModifiedTime;
    this.exists = exists;
    this.metastoreVersion = metastoreVersion;
  }

  public static MetastoreTableInfo of(TableInfo tableInfo, TableMetadataUnit unit, long metastoreVersion) {
    boolean exists = unit != null;
    Long lastModifiedTime = exists ? unit.lastModifiedTime() : null;
    return new MetastoreTableInfo(tableInfo, lastModifiedTime, exists, metastoreVersion);
  }

  @JsonProperty
  public TableInfo tableInfo() {
    return tableInfo;
  }

  @JsonProperty
  public Long lastModifiedTime() {
    return lastModifiedTime;
  }

  @JsonProperty
  public boolean isExists() {
    return exists;
  }

  @JsonProperty
  public long metastoreVersion() {
    return metastoreVersion;
  }

  /**
   * Checks if table metadata has changed or not, based on given exists status and last modified time.
   * Checks are done based on the following rules and order:
   * <ul>
   *   <li>If table did not exist but now does not, return true.</li>
   *   <li>If table existed but now does, return true.</li>
   *   <li>If both last modified times are null, return false.</li>
   *   <li>If one last modified time is null and other is not, return true.</li>
   *   <li>If both last modified times are the same, return false.</li>
   *   <li>If both last modified times are different, return true.</li>
   * </ul>
   *
   * @param currentExists current table exists status
   * @param currentLastModifiedTime current table lat modified time
   * @return true if table metadata has changed, false otherwise
   */
  public boolean hasChanged(boolean currentExists, Long currentLastModifiedTime) {
    if (exists && currentExists) {
      return !Objects.equals(lastModifiedTime, currentLastModifiedTime);
    } else {
      return exists || currentExists;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableInfo, lastModifiedTime, exists, metastoreVersion);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MetastoreTableInfo that = (MetastoreTableInfo) o;
    return exists == that.exists
      && metastoreVersion == that.metastoreVersion
      && Objects.equals(tableInfo, that.tableInfo)
      && Objects.equals(lastModifiedTime, that.lastModifiedTime);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", MetastoreTableInfo.class.getSimpleName() + "[", "]")
      .add("tableInfo=" + tableInfo)
      .add("lastModifiedTime=" + lastModifiedTime)
      .add("exists=" + exists)
      .add("metastoreVersion=" + metastoreVersion)
      .toString();
  }
}

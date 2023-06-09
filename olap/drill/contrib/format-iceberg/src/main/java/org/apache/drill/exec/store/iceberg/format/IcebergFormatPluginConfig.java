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
package org.apache.drill.exec.store.iceberg.format;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.exec.store.iceberg.snapshot.Snapshot;
import org.apache.drill.exec.store.iceberg.snapshot.SnapshotFactory;

import java.util.Map;
import java.util.Objects;

@JsonTypeName(IcebergFormatPluginConfig.NAME)
@JsonDeserialize(builder=IcebergFormatPluginConfig.IcebergFormatPluginConfigBuilder.class)
public class IcebergFormatPluginConfig implements FormatPluginConfig {

  public static final String NAME = "iceberg";

  private final Map<String, String> properties;

  private final Snapshot snapshot;

  private final Boolean caseSensitive;

  private final Boolean includeColumnStats;

  private final Boolean ignoreResiduals;

  private final Long snapshotId;

  private final Long snapshotAsOfTime;

  private final Long fromSnapshotId;

  private final Long toSnapshotId;

  @JsonCreator
  public IcebergFormatPluginConfig(
    IcebergFormatPluginConfigBuilder builder) {
    this.properties = builder.properties;
    this.caseSensitive = builder.caseSensitive;
    this.includeColumnStats = builder.includeColumnStats;
    this.ignoreResiduals = builder.ignoreResiduals;
    this.snapshotId = builder.snapshotId;
    this.snapshotAsOfTime = builder.snapshotAsOfTime;
    this.fromSnapshotId = builder.fromSnapshotId;
    this.toSnapshotId = builder.toSnapshotId;

    SnapshotFactory.SnapshotContext context = SnapshotFactory.SnapshotContext.builder()
      .snapshotId(snapshotId)
      .snapshotAsOfTime(snapshotAsOfTime)
      .fromSnapshotId(fromSnapshotId)
      .toSnapshotId(toSnapshotId)
      .build();

    this.snapshot = SnapshotFactory.INSTANCE.createSnapshot(context);
  }

  public static IcebergFormatPluginConfigBuilder builder() {
    return new IcebergFormatPluginConfigBuilder();
  }

  @JsonIgnore
  public Snapshot getSnapshot() {
    return snapshot;
  }

  public Map<String, String> getProperties() {
    return this.properties;
  }

  public Boolean getCaseSensitive() {
    return this.caseSensitive;
  }

  public Boolean getIncludeColumnStats() {
    return this.includeColumnStats;
  }

  public Boolean getIgnoreResiduals() {
    return this.ignoreResiduals;
  }

  public Long getSnapshotId() {
    return this.snapshotId;
  }

  public Long getSnapshotAsOfTime() {
    return this.snapshotAsOfTime;
  }

  public Long getFromSnapshotId() {
    return this.fromSnapshotId;
  }

  public Long getToSnapshotId() {
    return this.toSnapshotId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IcebergFormatPluginConfig that = (IcebergFormatPluginConfig) o;
    return Objects.equals(properties, that.properties)
      && Objects.equals(snapshot, that.snapshot)
      && Objects.equals(caseSensitive, that.caseSensitive)
      && Objects.equals(includeColumnStats, that.includeColumnStats)
      && Objects.equals(ignoreResiduals, that.ignoreResiduals)
      && Objects.equals(snapshotId, that.snapshotId)
      && Objects.equals(snapshotAsOfTime, that.snapshotAsOfTime)
      && Objects.equals(fromSnapshotId, that.fromSnapshotId)
      && Objects.equals(toSnapshotId, that.toSnapshotId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(properties, snapshot, caseSensitive, includeColumnStats,
      ignoreResiduals, snapshotId, snapshotAsOfTime, fromSnapshotId, toSnapshotId);
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class IcebergFormatPluginConfigBuilder {
    private Map<String, String> properties;

    private Boolean caseSensitive;

    private Boolean includeColumnStats;

    private Boolean ignoreResiduals;

    private Long snapshotId;

    private Long snapshotAsOfTime;

    private Long fromSnapshotId;

    private Long toSnapshotId;

    public IcebergFormatPluginConfigBuilder properties(Map<String, String> properties) {
      this.properties = properties;
      return this;
    }

    public IcebergFormatPluginConfigBuilder caseSensitive(Boolean caseSensitive) {
      this.caseSensitive = caseSensitive;
      return this;
    }

    public IcebergFormatPluginConfigBuilder includeColumnStats(Boolean includeColumnStats) {
      this.includeColumnStats = includeColumnStats;
      return this;
    }

    public IcebergFormatPluginConfigBuilder ignoreResiduals(Boolean ignoreResiduals) {
      this.ignoreResiduals = ignoreResiduals;
      return this;
    }

    public IcebergFormatPluginConfigBuilder snapshotId(Long snapshotId) {
      this.snapshotId = snapshotId;
      return this;
    }

    public IcebergFormatPluginConfigBuilder snapshotAsOfTime(Long snapshotAsOfTime) {
      this.snapshotAsOfTime = snapshotAsOfTime;
      return this;
    }

    public IcebergFormatPluginConfigBuilder fromSnapshotId(Long fromSnapshotId) {
      this.fromSnapshotId = fromSnapshotId;
      return this;
    }

    public IcebergFormatPluginConfigBuilder toSnapshotId(Long toSnapshotId) {
      this.toSnapshotId = toSnapshotId;
      return this;
    }

    public IcebergFormatPluginConfig build() {
      return new IcebergFormatPluginConfig(this);
    }
  }
}

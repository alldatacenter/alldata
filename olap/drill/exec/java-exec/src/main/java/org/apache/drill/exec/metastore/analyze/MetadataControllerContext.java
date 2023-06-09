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
package org.apache.drill.exec.metastore.analyze;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.metastore.components.tables.MetastoreTableInfo;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.hadoop.fs.Path;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Class which provides information required for storing metadata to the Metastore when performing analyze.
 */
@JsonDeserialize(builder = MetadataControllerContext.MetadataControllerContextBuilder.class)
public class MetadataControllerContext {
  private final TableInfo tableInfo;
  private final MetastoreTableInfo metastoreTableInfo;
  private final Path location;
  private final List<SchemaPath> interestingColumns;
  private final List<String> segmentColumns;
  private final List<MetadataInfo> metadataToHandle;
  private final List<MetadataInfo> metadataToRemove;
  private final MetadataType analyzeMetadataLevel;
  private final boolean multiValueSegments;

  private MetadataControllerContext(MetadataControllerContextBuilder builder) {
    this.tableInfo = builder.tableInfo;
    this.metastoreTableInfo = builder.metastoreTableInfo;
    this.location = builder.location;
    this.interestingColumns = builder.interestingColumns;
    this.segmentColumns = builder.segmentColumns;
    this.metadataToHandle = builder.metadataToHandle;
    this.metadataToRemove = builder.metadataToRemove;
    this.analyzeMetadataLevel = builder.analyzeMetadataLevel;
    this.multiValueSegments = builder.multiValueSegments;
  }

  @JsonProperty
  public TableInfo tableInfo() {
    return tableInfo;
  }

  @JsonProperty
  public MetastoreTableInfo metastoreTableInfo() {
    return metastoreTableInfo;
  }

  @JsonProperty
  public Path location() {
    return location;
  }

  @JsonProperty
  public List<SchemaPath> interestingColumns() {
    return interestingColumns;
  }

  @JsonProperty
  public List<String> segmentColumns() {
    return segmentColumns;
  }

  @JsonProperty
  public List<MetadataInfo> metadataToHandle() {
    return metadataToHandle;
  }

  @JsonProperty
  public List<MetadataInfo> metadataToRemove() {
    return metadataToRemove;
  }

  @JsonProperty
  public MetadataType analyzeMetadataLevel() {
    return analyzeMetadataLevel;
  }

  /**
   * Specifies whether metadata controller should create segments with multiple partition values.
   * For example, Hive partitions contain multiple partition values within the same segment.
   *
   * @return {@code true} if metadata controller should create segments with multiple partition values,
   * {@code false} otherwise
   */
  @JsonProperty
  public boolean multiValueSegments() {
    return multiValueSegments;
  }

  @Override
  public String toString() {
    return new StringJoiner(",\n", MetadataControllerContext.class.getSimpleName() + "[", "]")
        .add("tableInfo=" + tableInfo)
        .add("location=" + location)
        .add("interestingColumns=" + interestingColumns)
        .add("segmentColumns=" + segmentColumns)
        .add("metadataToHandle=" + metadataToHandle)
        .add("metadataToRemove=" + metadataToRemove)
        .add("analyzeMetadataLevel=" + analyzeMetadataLevel)
        .toString();
  }

  public static MetadataControllerContextBuilder builder() {
    return new MetadataControllerContextBuilder();
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class MetadataControllerContextBuilder {
    private TableInfo tableInfo;
    private MetastoreTableInfo metastoreTableInfo;
    private Path location;
    private List<SchemaPath> interestingColumns;
    private List<String> segmentColumns;
    private List<MetadataInfo> metadataToHandle;
    private List<MetadataInfo> metadataToRemove;
    private MetadataType analyzeMetadataLevel;
    private boolean multiValueSegments;

    public MetadataControllerContextBuilder tableInfo(TableInfo tableInfo) {
      this.tableInfo = tableInfo;
      return this;
    }

    public MetadataControllerContextBuilder metastoreTableInfo(MetastoreTableInfo metastoreTableInfo) {
      this.metastoreTableInfo = metastoreTableInfo;
      return this;
    }

    public MetadataControllerContextBuilder location(Path location) {
      this.location = location;
      return this;
    }

    public MetadataControllerContextBuilder interestingColumns(List<SchemaPath> interestingColumns) {
      this.interestingColumns = interestingColumns;
      return this;
    }

    public MetadataControllerContextBuilder segmentColumns(List<String> segmentColumns) {
      this.segmentColumns = segmentColumns;
      return this;
    }

    public MetadataControllerContextBuilder metadataToHandle(List<MetadataInfo> metadataToHandle) {
      this.metadataToHandle = metadataToHandle;
      return this;
    }

    public MetadataControllerContextBuilder metadataToRemove(List<MetadataInfo> metadataToRemove) {
      this.metadataToRemove = metadataToRemove;
      return this;
    }

    public MetadataControllerContextBuilder analyzeMetadataLevel(MetadataType metadataType) {
      this.analyzeMetadataLevel = metadataType;
      return this;
    }

    public MetadataControllerContextBuilder multiValueSegments(boolean multiValueSegments) {
      this.multiValueSegments = multiValueSegments;
      return this;
    }

    public MetadataControllerContext build() {
      Objects.requireNonNull(tableInfo, "tableInfo was not set");
      Objects.requireNonNull(location, "location was not set");
      Objects.requireNonNull(segmentColumns, "segmentColumns were not set");
      Objects.requireNonNull(metadataToRemove, "metadataToRemove was not set");
      return new MetadataControllerContext(this);
    }
  }
}

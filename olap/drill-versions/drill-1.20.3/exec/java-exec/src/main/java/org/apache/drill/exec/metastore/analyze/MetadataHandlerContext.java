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
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.TableInfo;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Class which provides information required for handling results of metadata aggregation when performing analyze.
 */
@JsonDeserialize(builder = MetadataHandlerContext.MetadataHandlerContextBuilder.class)
public class MetadataHandlerContext {
  private final TableInfo tableInfo;
  private final List<MetadataInfo> metadataToHandle;
  private final MetadataType metadataType;
  private final int depthLevel;
  private final List<String> segmentColumns;

  private MetadataHandlerContext(MetadataHandlerContextBuilder builder) {
    this.tableInfo = builder.tableInfo;
    this.metadataToHandle = builder.metadataToHandle;
    this.metadataType = builder.metadataType;
    this.depthLevel = builder.depthLevel;
    this.segmentColumns = builder.segmentColumns;
  }

  @JsonProperty
  public TableInfo tableInfo() {
    return tableInfo;
  }

  @JsonProperty
  public List<MetadataInfo> metadataToHandle() {
    return metadataToHandle;
  }

  @JsonProperty
  public MetadataType metadataType() {
    return metadataType;
  }

  @JsonProperty
  public int depthLevel() {
    return depthLevel;
  }

  @JsonProperty
  public List<String> segmentColumns() {
    return segmentColumns;
  }

  @Override
  public String toString() {
    return new StringJoiner(",\n", MetadataHandlerContext.class.getSimpleName() + "[", "]")
        .add("tableInfo=" + tableInfo)
        .add("metadataToHandle=" + metadataToHandle)
        .add("metadataType=" + metadataType)
        .add("depthLevel=" + depthLevel)
        .add("segmentColumns=" + segmentColumns)
        .toString();
  }

  public static MetadataHandlerContextBuilder builder() {
    return new MetadataHandlerContextBuilder();
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class MetadataHandlerContextBuilder {
    private TableInfo tableInfo;
    private List<MetadataInfo> metadataToHandle;
    private MetadataType metadataType;
    private Integer depthLevel;
    private List<String> segmentColumns;

    public MetadataHandlerContextBuilder tableInfo(TableInfo tableInfo) {
      this.tableInfo = tableInfo;
      return this;
    }

    public MetadataHandlerContextBuilder metadataToHandle(List<MetadataInfo> metadataToHandle) {
      this.metadataToHandle = metadataToHandle;
      return this;
    }

    public MetadataHandlerContextBuilder metadataType(MetadataType metadataType) {
      this.metadataType = metadataType;
      return this;
    }

    public MetadataHandlerContextBuilder depthLevel(int depthLevel) {
      this.depthLevel = depthLevel;
      return this;
    }

    public MetadataHandlerContextBuilder segmentColumns(List<String> segmentColumns) {
      this.segmentColumns = segmentColumns;
      return this;
    }

    public MetadataHandlerContext build() {
      Objects.requireNonNull(tableInfo, "tableInfo was not set");
      Objects.requireNonNull(metadataToHandle, "metadataToHandle was not set");
      Objects.requireNonNull(metadataType, "metadataType was not set");
      Objects.requireNonNull(depthLevel, "depthLevel was not set");
      Objects.requireNonNull(segmentColumns, "segmentColumns were not set");
      return new MetadataHandlerContext(this);
    }
  }
}

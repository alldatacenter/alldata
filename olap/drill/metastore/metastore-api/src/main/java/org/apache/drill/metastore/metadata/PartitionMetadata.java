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
package org.apache.drill.metastore.metadata;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.hadoop.fs.Path;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Represents a metadata for the table part, which corresponds to the specific partition key.
 */
public class PartitionMetadata extends BaseMetadata {
  private final SchemaPath column;
  private final List<String> partitionValues;
  private final Set<Path> locations;

  private PartitionMetadata(PartitionMetadataBuilder builder) {
    super(builder);
    this.column = builder.column;
    this.partitionValues = builder.partitionValues;
    this.locations = builder.locations;
  }

  /**
   * It allows to obtain the column path for this partition
   *
   * @return column path
   */
  public SchemaPath getColumn() {
    return column;
  }

  /**
   * File locations for this partition
   *
   * @return file locations
   */
  public Set<Path> getLocations() {
    return locations;
  }

  public List<String> getPartitionValues() {
    return partitionValues;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    PartitionMetadata that = (PartitionMetadata) o;
    return Objects.equals(column, that.column)
        && Objects.equals(partitionValues, that.partitionValues)
        && Objects.equals(locations, that.locations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), column, partitionValues, locations);
  }

  @Override
  public String toString() {
    return new StringJoiner(",\n", PartitionMetadata.class.getSimpleName() + "[\n", "]")
        .add("column=" + column)
        .add("partitionValues=" + partitionValues)
        .add("locations=" + locations)
        .add("tableInfo=" + tableInfo)
        .add("metadataInfo=" + metadataInfo)
        .add("schema=" + schema)
        .add("columnsStatistics=" + columnsStatistics)
        .add("metadataStatistics=" + metadataStatistics)
        .add("lastModifiedTime=" + lastModifiedTime)
        .toString();
  }

  @Override
  protected void toMetadataUnitBuilder(TableMetadataUnit.Builder builder) {
    builder.column(column.toString());
    builder.partitionValues(partitionValues);
    builder.locations(locations.stream()
      .map(location -> location.toUri().getPath())
      .collect(Collectors.toList()));
  }

  public PartitionMetadataBuilder toBuilder() {
    return builder()
        .tableInfo(tableInfo)
        .metadataInfo(metadataInfo)
        .schema(schema)
        .columnsStatistics(columnsStatistics)
        .metadataStatistics(metadataStatistics.values())
        .lastModifiedTime(lastModifiedTime)
        .column(column)
        .partitionValues(partitionValues)
        .locations(locations);
  }

  public static PartitionMetadataBuilder builder() {
    return new PartitionMetadataBuilder();
  }

  public static class PartitionMetadataBuilder extends BaseMetadataBuilder<PartitionMetadataBuilder> {
    private SchemaPath column;
    private List<String> partitionValues;
    private Set<Path> locations;

    public PartitionMetadataBuilder locations(Set<Path> locations) {
      this.locations = locations;
      return self();
    }

    public PartitionMetadataBuilder partitionValues(List<String> partitionValues) {
      this.partitionValues = partitionValues;
      return self();
    }

    public PartitionMetadataBuilder column(SchemaPath column) {
      this.column = column;
      return self();
    }

    @Override
    protected void checkRequiredValues() {
      super.checkRequiredValues();
      Objects.requireNonNull(column, "column was not set");
      Objects.requireNonNull(partitionValues, "partitionValues were not set");
      Objects.requireNonNull(locations, "locations were not set");
    }

    @Override
    public PartitionMetadata build() {
      checkRequiredValues();
      return new PartitionMetadata(this);
    }

    @Override
    protected PartitionMetadataBuilder self() {
      return this;
    }

    @Override
    protected PartitionMetadataBuilder metadataUnitInternal(TableMetadataUnit unit) {
      if (unit.locations() != null) {
        locations(unit.locations().stream()
          .map(Path::new)
          .collect(Collectors.toSet()));
      }
      partitionValues(unit.partitionValues());
      column(SchemaPath.parseFromString(unit.column()));
      return self();
    }
  }
}

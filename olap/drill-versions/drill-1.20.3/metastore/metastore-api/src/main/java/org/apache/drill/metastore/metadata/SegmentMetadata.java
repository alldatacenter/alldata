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
 * Metadata which corresponds to the segment level of table.
 */
public class SegmentMetadata extends BaseMetadata implements LocationProvider {
  private final SchemaPath column;
  private final Path path;
  private final List<String> partitionValues;
  private final Set<Path> locations;

  private SegmentMetadata(SegmentMetadataBuilder builder) {
    super(builder);
    this.column = builder.column;
    this.path = builder.path;
    this.partitionValues = builder.partitionValues;
    this.locations = builder.locations;
  }

  public SchemaPath getColumn() {
    return column;
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public Path getLocation() {
    return path;
  }

  public List<String> getPartitionValues() {
    return partitionValues;
  }

  public Set<Path> getLocations() {
    return locations;
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
    SegmentMetadata that = (SegmentMetadata) o;
    return Objects.equals(column, that.column)
        && Objects.equals(path, that.path)
        && Objects.equals(partitionValues, that.partitionValues)
        && Objects.equals(locations, that.locations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), column, path, partitionValues, locations);
  }

  @Override
  public String toString() {
    return new StringJoiner(",\n", SegmentMetadata.class.getSimpleName() + "[\n", "]")
        .add("column=" + column)
        .add("path=" + path)
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
    if (column != null) {
      builder.column(column.toString());
    }
    builder.path(path.toUri().getPath());
    builder.location(getLocation().toUri().getPath());
    builder.partitionValues(partitionValues);
    builder.locations(locations.stream()
      .map(location -> location.toUri().getPath())
      .collect(Collectors.toList()));
  }

  public SegmentMetadataBuilder toBuilder() {
    return builder()
        .tableInfo(tableInfo)
        .metadataInfo(metadataInfo)
        .path(path)
        .locations(locations)
        .schema(schema)
        .columnsStatistics(columnsStatistics)
        .metadataStatistics(metadataStatistics.values())
        .lastModifiedTime(lastModifiedTime)
        .column(column)
        .partitionValues(partitionValues);
  }

  public static SegmentMetadataBuilder builder() {
    return new SegmentMetadataBuilder();
  }

  public static class SegmentMetadataBuilder extends BaseMetadataBuilder<SegmentMetadataBuilder> {
    private SchemaPath column;
    private List<String> partitionValues;
    private Path path;
    private Set<Path> locations;

    public SegmentMetadataBuilder locations(Set<Path> locations) {
      this.locations = locations;
      return self();
    }

    public SegmentMetadataBuilder path(Path path) {
      this.path = path;
      return self();
    }

    public SegmentMetadataBuilder partitionValues(List<String> partitionValues) {
      this.partitionValues = partitionValues;
      return self();
    }

    public SegmentMetadataBuilder column(SchemaPath column) {
      this.column = column;
      return self();
    }

    @Override
    protected void checkRequiredValues() {
      super.checkRequiredValues();
      Objects.requireNonNull(path, "path was not set");
      Objects.requireNonNull(locations, "locations were not set");
    }

    @Override
    public SegmentMetadata build() {
      checkRequiredValues();
      return new SegmentMetadata(this);
    }

    @Override
    protected SegmentMetadataBuilder self() {
      return this;
    }

    @Override
    protected SegmentMetadataBuilder metadataUnitInternal(TableMetadataUnit unit) {
      if (unit.locations() != null) {
        locations(unit.locations().stream()
          .map(Path::new)
          .collect(Collectors.toSet()));
      }
      if (unit.path() != null) {
        path(new Path(unit.path()));
      }
      partitionValues(unit.partitionValues());
      column(SchemaPath.parseFromString(unit.column()));
      return self();
    }
  }
}

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

import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.hadoop.fs.Path;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Metadata which corresponds to the file level of table.
 */
public class FileMetadata extends BaseMetadata implements LocationProvider {
  private final Path path;

  private FileMetadata(FileMetadataBuilder builder) {
    super(builder);
    this.path = builder.path;
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public Path getLocation() {
    return path.getParent();
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
    FileMetadata that = (FileMetadata) o;
    return Objects.equals(path, that.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), path);
  }

  @Override
  public String toString() {
    return new StringJoiner(",\n", FileMetadata.class.getSimpleName() + "[\n", "]")
        .add("path=" + path)
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
    builder.path(path.toUri().getPath());
    builder.location(getLocation().toUri().getPath());
  }

  public FileMetadataBuilder toBuilder() {
    return builder()
        .tableInfo(tableInfo)
        .metadataInfo(metadataInfo)
        .schema(schema)
        .columnsStatistics(columnsStatistics)
        .metadataStatistics(metadataStatistics.values())
        .lastModifiedTime(lastModifiedTime)
        .path(path);
  }

  public static FileMetadataBuilder builder() {
    return new FileMetadataBuilder();
  }

  public static class FileMetadataBuilder extends BaseMetadataBuilder<FileMetadataBuilder> {
    private Path path;

    public FileMetadataBuilder path(Path path) {
      this.path = path;
      return self();
    }

    @Override
    protected void checkRequiredValues() {
      super.checkRequiredValues();
      Objects.requireNonNull(path, "path was not set");
    }

    @Override
    public FileMetadata build() {
      checkRequiredValues();
      return new FileMetadata(this);
    }

    @Override
    protected FileMetadataBuilder self() {
      return this;
    }

    @Override
    protected FileMetadataBuilder metadataUnitInternal(TableMetadataUnit unit) {
      if (unit.path() != null) {
        path(new Path(unit.path()));
      }
      return self();
    }
  }
}

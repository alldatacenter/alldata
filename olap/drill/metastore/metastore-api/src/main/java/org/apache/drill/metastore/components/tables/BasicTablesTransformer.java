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

import org.apache.drill.metastore.metadata.BaseTableMetadata;
import org.apache.drill.metastore.metadata.FileMetadata;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.PartitionMetadata;
import org.apache.drill.metastore.metadata.RowGroupMetadata;
import org.apache.drill.metastore.metadata.SegmentMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Basic metadata transformer class which can transform given list of {@link TableMetadataUnit}
 * into {@link BaseTableMetadata}, {@link SegmentMetadata}, {@link FileMetadata},
 * {@link RowGroupMetadata}, {@link PartitionMetadata} or all metadata types returned in one holder
 * ({@link MetadataHolder}).
 */
public class BasicTablesTransformer {

  public static List<BaseTableMetadata> tables(List<TableMetadataUnit> units) {
    return units.stream()
      .filter(unit -> MetadataType.TABLE == MetadataType.fromValue(unit.metadataType()))
      .map(unit -> BaseTableMetadata.builder().metadataUnit(unit).build())
      .collect(Collectors.toList());
  }

  public static List<SegmentMetadata> segments(List<TableMetadataUnit> units) {
    return units.stream()
      .filter(unit -> MetadataType.SEGMENT == MetadataType.fromValue(unit.metadataType()))
      .map(unit -> SegmentMetadata.builder().metadataUnit(unit).build())
      .collect(Collectors.toList());
  }

  public static List<FileMetadata> files(List<TableMetadataUnit> units) {
    return units.stream()
      .filter(unit -> MetadataType.FILE == MetadataType.fromValue(unit.metadataType()))
      .map(unit -> FileMetadata.builder().metadataUnit(unit).build())
      .collect(Collectors.toList());
  }

  public static List<RowGroupMetadata> rowGroups(List<TableMetadataUnit> units) {
    return units.stream()
      .filter(unit -> MetadataType.ROW_GROUP == MetadataType.fromValue(unit.metadataType()))
      .map(unit -> RowGroupMetadata.builder().metadataUnit(unit).build())
      .collect(Collectors.toList());
  }

  public static List<PartitionMetadata> partitions(List<TableMetadataUnit> units) {
    return units.stream()
      .filter(unit -> MetadataType.PARTITION == MetadataType.fromValue(unit.metadataType()))
      .map(unit -> PartitionMetadata.builder().metadataUnit(unit).build())
      .collect(Collectors.toList());
  }

  public static MetadataHolder all(List<TableMetadataUnit> units) {
    List<BaseTableMetadata> tables = new ArrayList<>();
    List<SegmentMetadata> segments = new ArrayList<>();
    List<FileMetadata> files = new ArrayList<>();
    List<RowGroupMetadata> rowGroups = new ArrayList<>();
    List<PartitionMetadata> partitions = new ArrayList<>();

    for (TableMetadataUnit unit : units) {
      MetadataType metadataType = MetadataType.fromValue(unit.metadataType());
      if (metadataType == null) {
        continue;
      }
      switch (metadataType) {
        case TABLE:
          tables.add(BaseTableMetadata.builder().metadataUnit(unit).build());
          break;
        case SEGMENT:
          segments.add(SegmentMetadata.builder().metadataUnit(unit).build());
          break;
        case FILE:
          files.add(FileMetadata.builder().metadataUnit(unit).build());
          break;
        case ROW_GROUP:
          rowGroups.add(RowGroupMetadata.builder().metadataUnit(unit).build());
          break;
        case PARTITION:
          partitions.add(PartitionMetadata.builder().metadataUnit(unit).build());
          break;
        default:
          // Ignore unsupported type
          break;
      }
    }
    return new MetadataHolder(tables, segments, files, rowGroups, partitions);
  }

  public static class MetadataHolder {

    private final List<BaseTableMetadata> tables;
    private final List<SegmentMetadata> segments;
    private final List<FileMetadata> files;
    private final List<RowGroupMetadata> rowGroups;
    private final List<PartitionMetadata> partitions;

    public MetadataHolder(List<BaseTableMetadata> tables,
                          List<SegmentMetadata> segments,
                          List<FileMetadata> files,
                          List<RowGroupMetadata> rowGroups,
                          List<PartitionMetadata> partitions) {
      this.tables = tables;
      this.segments = segments;
      this.files = files;
      this.rowGroups = rowGroups;
      this.partitions = partitions;
    }

    public List<BaseTableMetadata> tables() {
      return tables;
    }

    public List<SegmentMetadata> segments() {
      return segments;
    }

    public List<FileMetadata> files() {
      return files;
    }

    public List<RowGroupMetadata> rowGroups() {
      return rowGroups;
    }

    public List<PartitionMetadata> partitions() {
      return partitions;
    }
  }
}

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
import org.apache.hadoop.fs.Path;

import java.util.List;
import java.util.Map;

/**
 * Base interface for providing table, partition, file etc. metadata for specific table.
 *
 * getPartitionsMetadata(), getFilesMetadata() and other methods which return collections return
 * empty collection instead of null for the case when required metadata level is not available,
 * or cannot re obtained using current {@link TableMetadataProvider} instance.
 */
public interface TableMetadataProvider {

  /**
   * Returns {@link TableMetadata} instance which provides metadata for table and columns metadata.
   *
   * @return {@link TableMetadata} instance
   */
  TableMetadata getTableMetadata();

  /**
   * Returns list of partition columns for table from this {@link TableMetadataProvider}.
   *
   * @return list of partition columns
   */
  List<SchemaPath> getPartitionColumns();

  /**
   * Returns list of {@link PartitionMetadata} instances which provides metadata for specific partitions and its columns.
   *
   * @return list of {@link PartitionMetadata} instances
   */
  List<PartitionMetadata> getPartitionsMetadata();

  /**
   * Returns list of {@link PartitionMetadata} instances which corresponds to partitions for specified column
   * and provides metadata for specific partitions and its columns.
   *
   * @return list of {@link PartitionMetadata} instances which corresponds to partitions for specified column
   */
  List<PartitionMetadata> getPartitionMetadata(SchemaPath columnName);

  /**
   * Returns map of {@link FileMetadata} instances which provides metadata for specific file and its columns.
   *
   * @return map of {@link FileMetadata} instances
   */
  Map<Path, FileMetadata> getFilesMetadataMap();

  /**
   * Returns map of {@link SegmentMetadata} instances which provides metadata for segment and its columns.
   *
   * @return map of {@link SegmentMetadata} instances
   */
  Map<Path, SegmentMetadata> getSegmentsMetadataMap();

  /**
   * Returns {@link FileMetadata} instance which corresponds to metadata of file for specified location.
   *
   * @param location location of the file
   * @return {@link FileMetadata} instance which corresponds to metadata of file for specified location
   */
  FileMetadata getFileMetadata(Path location);

  /**
   * Returns list of {@link FileMetadata} instances which belongs to specified partitions.
   *
   * @param partition partition which
   * @return list of {@link FileMetadata} instances which belongs to specified partitions
   */
  List<FileMetadata> getFilesForPartition(PartitionMetadata partition);

  /**
   * Returns {@link NonInterestingColumnsMetadata} instance which provides metadata for non-interesting columns.
   *
   * @return {@link NonInterestingColumnsMetadata} instance
   */
  NonInterestingColumnsMetadata getNonInterestingColumnsMetadata();

  /**
   * Whether metadata actuality should be checked.
   *
   * @return true if metadata actuality should be checked
   */
  boolean checkMetadataVersion();
}

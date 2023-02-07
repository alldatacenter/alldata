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
package org.apache.drill.exec.metastore.store.parquet;

import org.apache.drill.exec.store.dfs.ReadEntryWithPath;
import org.apache.drill.metastore.metadata.RowGroupMetadata;
import org.apache.drill.metastore.metadata.TableMetadataProvider;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;
import org.apache.hadoop.fs.Path;

import java.util.List;
import java.util.Set;

/**
 * Interface for providing table, partition, file etc. metadata for specific table with parquet files.
 */
public interface ParquetMetadataProvider extends TableMetadataProvider {

  /**
   * Returns list of {@link ReadEntryWithPath} instances which represents paths to files to be scanned.
   *
   * @return list of {@link ReadEntryWithPath} instances whith file paths
   */
  List<ReadEntryWithPath> getEntries();

  /**
   * Returns list of {@link RowGroupMetadata} instances which provides metadata for specific row group and its columns.
   *
   * @return list of {@link RowGroupMetadata} instances
   */
  List<RowGroupMetadata> getRowGroupsMeta();

  /**
   * Returns list of file paths which belong to current table.
   *
   * @return list of file paths
   */
  List<Path> getLocations();

  /**
   * Returns multimap of {@link RowGroupMetadata} instances which provides metadata
   * for specific row group and its columns mapped to their locations.
   *
   * @return multimap of {@link RowGroupMetadata} instances
   */
  Multimap<Path, RowGroupMetadata> getRowGroupsMetadataMap();

  /**
   * Returns list of file locations for table.
   *
   * @return list of file locations for table
   */
  Set<Path> getFileSet();
}

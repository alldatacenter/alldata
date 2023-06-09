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

import org.apache.calcite.rel.core.TableScan;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;

import java.util.List;

/**
 * Interface for obtaining information about segments, files etc which should be handled in Metastore
 * when producing incremental analyze.
 */
public interface MetadataInfoCollector {

  /**
   * Returns list of row groups metadata info which should be fetched from the Metastore.
   *
   * @return list of row groups metadata info
   */
  List<MetadataInfo> getRowGroupsInfo();

  /**
   * Returns list of files metadata info which should be fetched from the Metastore.
   *
   * @return list of files metadata info
   */
  List<MetadataInfo> getFilesInfo();

  /**
   * Returns list of segments metadata info which should be fetched from the Metastore.
   *
   * @return list of segments metadata info
   */
  Multimap<Integer, MetadataInfo> getSegmentsInfo();

  /**
   * Returns list of all metadata info instances which should be handled
   * either producing analyze or when fetching from the Metastore.
   *
   * @return list of all metadata info
   */
  List<MetadataInfo> getAllMetaToHandle();

  /**
   * Returns list of all metadata info which corresponds to top-level segments and should be removed from the Metastore.
   *
   * @return list of all metadata info which should be removed
   */
  List<MetadataInfo> getMetadataToRemove();

  /**
   * Returns {@link TableScan} instance which will be used when produced incremental analyze.
   * Table scan will contain minimal selection required for obtaining correct metadata.
   *
   * @return {@link TableScan} instance
   */
  TableScan getPrunedScan();

  /**
   * Returns true if table metadata is outdated.
   *
   * @return true if table metadata is outdated
   */
  boolean isOutdated();
}

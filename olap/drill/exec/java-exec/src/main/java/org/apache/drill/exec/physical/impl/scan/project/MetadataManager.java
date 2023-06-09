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
package org.apache.drill.exec.physical.impl.scan.project;

import org.apache.drill.exec.physical.impl.scan.project.ScanLevelProjection.ScanProjectionParser;
import org.apache.drill.exec.physical.impl.scan.project.ReaderLevelProjection.ReaderProjectionResolver;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;

/**
 * Queries can contain a wildcard (*), table columns, or special
 * system-defined columns (the file metadata columns AKA implicit
 * columns, the `columns` column of CSV, etc.).
 * <p>
 * This class provides a generalized way of handling such extended
 * columns. That is, this handles metadata for columns defined by
 * the scan or file; columns defined by the table (the actual
 * data metadata) is handled elsewhere.
 * <p>
 * Objects of this interface are driven by the projection processing
 * framework which provides a vector cache from which to obtain
 * materialized columns. The implementation must provide a projection
 * parser to pick out the columns which this object handles.
 * <p>
 * A better name might be ImplicitMetadataManager to signify that
 * this is about metadata other than table columns.
 */
public interface MetadataManager {

  void bind(ResultVectorCache vectorCache);

  ScanProjectionParser projectionParser();

  ReaderProjectionResolver resolver();

  /**
   * Define (materialize) the columns which this manager
   * represents.
   */
  void define();

  /**
   * Load data into the custom columns, if needed (such as for
   * null or implicit columns.)
   *
   * @param rowCount number of rows read into a batch.
   */
  void load(int rowCount);

  /**
   * Event indicating the end of a file (or other data source.)
   */
  void endFile();

  /**
   * Event indicating the end of a scan.
   */
  void close();
}

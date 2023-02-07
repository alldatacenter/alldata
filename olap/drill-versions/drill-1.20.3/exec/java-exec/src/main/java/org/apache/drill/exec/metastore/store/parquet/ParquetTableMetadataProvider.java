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

import org.apache.hadoop.fs.Path;

/**
 * Interface for providing table, partition, file etc. metadata for specific parquet table.
 */
public interface ParquetTableMetadataProvider extends ParquetMetadataProvider {

  /**
   * Whether metadata cache files are used for table which belongs to current metadata provider.
   *
   * @return true if metadata cache files are used
   */
  boolean isUsedMetadataCache();

  /**
   * Returns root table path.
   *
   * @return root path of the table
   */
  Path getSelectionRoot();
}

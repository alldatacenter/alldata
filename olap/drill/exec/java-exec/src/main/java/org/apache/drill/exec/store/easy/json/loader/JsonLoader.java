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
package org.apache.drill.exec.store.easy.json.loader;

import org.apache.drill.exec.record.metadata.ColumnMetadata;

/**
 * Enhanced second-generation JSON loader which takes an input
 * source and creates a series of record batches using the
 * {@link org.apache.drill.exec.physical.resultSet.ResultSetLoader
 * ResultSetLoader} abstraction.
 */
public interface JsonLoader {

  /**
   * Column property specific to the JSON loader.
   * Mode for reading Varchar columns from JSON. One of:
   * <li>
   * <li>{@link #JSON_TYPED_MODE}: Read using normal typing rules
   * (default).</li>
   * <li>{@link #JSON_TEXT_MODE}: Like the JSON format plugin's
   * "all-text mode", but for a single column. That JSON field is
   * read as text regardless of the actual value. Applies only to
   * scalars.</li>
   * <li>{@link #JSON_LITERAL_MODE}: Causes the field, and all its
   * children, to be read as literal JSON: the values are returned
   * as a valid JSON string.</li>
   * </li>
   */
  String JSON_MODE = ColumnMetadata.DRILL_PROP_PREFIX + "json-mode";
  String JSON_TEXT_MODE = "text";
  String JSON_TYPED_MODE = "typed";
  String JSON_LITERAL_MODE = "json";

  /**
   * Read one batch of row data.
   *
   * @return {@code true} if at least one record was loaded, {@code false} if EOF.
   * @throws {org.apache.drill.common.exceptions.UserException
   * for most errors
   * @throws RuntimeException for unexpected errors, most often due
   * to code errors
   */
  boolean readBatch();

  /**
   * Releases resources held by this class including the input stream.
   * Does not close the result set loader passed into this instance.
   */
  void close();
}

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
package org.apache.drill.exec.record.metadata.schema;

import java.io.IOException;
import java.util.Map;

/**
 * Provides mechanisms to manage schema: store / read / delete.
 */
public interface SchemaProvider {

  /**
   * Default schema file name where schema is stored on file system.
   * File is hidden to avoid including it when reading table data.
   */
  String DEFAULT_SCHEMA_NAME = ".drill.schema";

  /**
   * Deletes schema.
   */
  void delete() throws IOException;

  /**
   * Stores given schema definition and properties.
   *
   * @param schema schema definition
   * @param properties map of properties
   * @param storageProperties storage properties
   */
  void store(String schema, Map<String, String> properties, StorageProperties storageProperties) throws IOException;

  /**
   * Reads schema into {@link SchemaContainer}. Depending on implementation, can read from a file
   * or from the given input.
   *
   * @return table schema instance
   */
  SchemaContainer read() throws IOException;

  /**
   * Checks if schema exists.
   *
   * @return true if schema exists, false otherwise
   */
  boolean exists() throws IOException;
}

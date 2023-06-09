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
import org.apache.drill.exec.store.easy.json.parser.ElementParser;
import org.apache.drill.exec.store.easy.json.parser.ValueParser;

/**
 * Extensible mechanism to build fields for a JSON object (a Drill
 * row or Map).
 */
public interface FieldFactory {

  /**
   * Create a parser for a field. The caller will add the field
   * to the parent object.
   * Called only for projected fields. May add a "deferred"
   * undefined field if the value type is undefined. Such fields are added
   * to the underlying row or map at a later time.
   */
  ElementParser fieldParser(FieldDefn fieldDefn);

  ElementParser ignoredFieldParser();

  ElementParser forceNullResolution(FieldDefn fieldDefn);
  ElementParser forceArrayResolution(FieldDefn fieldDefn);

  /**
   * Internal method which allows a custom parser (such as for
   * extended types) to provide the scalar parser for a provided
   * schema.
   */
  ValueParser scalarParserFor(FieldDefn fieldDefn, ColumnMetadata colSchema);
}

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
package org.apache.drill.exec.record;

import org.apache.drill.exec.record.metadata.SchemaBuilder;

public class BatchSchemaBuilder {
  private BatchSchema.SelectionVectorMode svMode = BatchSchema.SelectionVectorMode.NONE;
  private SchemaBuilder schemaBuilder;

  public BatchSchemaBuilder() {
  }

  /**
   * Create a new schema starting with the base schema. Allows appending
   * additional columns to an additional schema.
   */
  public BatchSchemaBuilder(BatchSchema baseSchema) {
    schemaBuilder = new SchemaBuilder();
    for (MaterializedField field : baseSchema) {
      schemaBuilder.add(field);
    }
  }

  public BatchSchemaBuilder withSVMode(BatchSchema.SelectionVectorMode svMode) {
    this.svMode = svMode;
    return this;
  }

  public BatchSchemaBuilder withSchemaBuilder(SchemaBuilder schemaBuilder) {
    this.schemaBuilder = schemaBuilder;
    return this;
  }

  public SchemaBuilder schemaBuilder() {
    return schemaBuilder;
  }

  public BatchSchema build() {
    return new BatchSchema(svMode, schemaBuilder.buildSchema().toFieldList());
  }
}

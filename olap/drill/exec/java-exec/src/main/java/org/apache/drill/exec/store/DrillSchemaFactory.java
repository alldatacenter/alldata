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
package org.apache.drill.exec.store;

import java.io.IOException;

import org.apache.calcite.schema.SchemaPlus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DrillSchemaFactory extends AbstractSchemaFactory {
  private static final Logger logger = LoggerFactory.getLogger(DrillSchemaFactory.class);

  public DrillSchemaFactory(String name) {
    super(name);
  }

  /**
   * Historical note.  This method used to eagerly register schemas for every
   * enabled storage plugin instance, an operation that is expensive at best
   * and unreliable in the presence of unresponsive storage plugins at worst.
   * Now the schemas under the root are registered lazily by DynamicRootSchema.
   *
   * @param schemaConfig Configuration for schema objects.
   * @param parent Reference to parent schema.
   * @throws IOException
   */
  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) throws IOException {
    throw new UnsupportedOperationException("Only lazy schema registration by DynamicRootSchema is supported.");
  }
}

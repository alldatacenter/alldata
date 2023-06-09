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
package org.apache.drill.exec.record.selection;

import java.util.List;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.RecordBatch;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class SelectionVector4Builder {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SelectionVector4Builder.class);

  private List<BatchSchema> schemas = Lists.newArrayList();

  public void add(RecordBatch batch, boolean newSchema) throws SchemaChangeException {
    if (!schemas.isEmpty() && newSchema) {
      throw new SchemaChangeException("Currently, the sv4 builder doesn't support embedded types");
    }
    if (newSchema) {
      schemas.add(batch.getSchema());
    }
  }

  // deals with managing selection vectors.
  // take a four byte int
  /**
   * take a four byte value
   * use the first two as a pointer.  use the other two as a
   *
   *  we should manage an array of valuevectors
   */

  private class VectorSchemaBuilder {
  }

}

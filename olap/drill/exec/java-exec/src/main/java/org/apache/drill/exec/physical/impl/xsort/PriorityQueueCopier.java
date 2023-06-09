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
package org.apache.drill.exec.physical.impl.xsort;

import java.io.IOException;
import java.util.List;

import org.apache.drill.exec.compile.TemplateClassDefinition;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.VectorAccessible;

public interface PriorityQueueCopier extends AutoCloseable {
  TemplateClassDefinition<PriorityQueueCopier> TEMPLATE_DEFINITION =
      new TemplateClassDefinition<>(PriorityQueueCopier.class, PriorityQueueCopierTemplate.class);

  void setup(BufferAllocator allocator, VectorAccessible hyperBatch,
      List<BatchGroup> batchGroups, VectorAccessible outgoing) throws SchemaChangeException;

  int next(int targetRecordCount);

  @Override
  void close() throws IOException; // specify this to leave out the Exception
}

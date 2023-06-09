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
package org.apache.drill.exec.physical.resultSet;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.vector.ValueVector;

/**
 * Interface for a cache that implements "vector persistence" across
 * multiple result set loaders. Allows a single scan operator to offer
 * the same set of vectors even when data is read by a set of readers.
 */

public interface ResultVectorCache {
  BufferAllocator allocator();
  ValueVector vectorFor(MaterializedField colSchema);
  MajorType getType(String name);
  boolean isPermissive();
  ResultVectorCache childCache(String colName);
}

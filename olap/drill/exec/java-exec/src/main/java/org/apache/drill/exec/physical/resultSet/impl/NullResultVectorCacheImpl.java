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
package org.apache.drill.exec.physical.resultSet.impl;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.vector.ValueVector;

/**
 * A vector cache implementation which does not actually cache.
 */

public class NullResultVectorCacheImpl implements ResultVectorCache {

  private final BufferAllocator allocator;

  public NullResultVectorCacheImpl(BufferAllocator allocator) {
    this.allocator = allocator;
  }

  @Override
  public BufferAllocator allocator() { return allocator; }

  @Override
  public ValueVector vectorFor(MaterializedField colSchema) {
    return TypeHelper.getNewVector(colSchema, allocator, null);
  }

  @Override
  public MajorType getType(String name) { return null; }

  @Override
  public boolean isPermissive() { return false; }

  @Override
  public ResultVectorCache childCache(String colName) {
    return new NullResultVectorCacheImpl(allocator);
  }
}

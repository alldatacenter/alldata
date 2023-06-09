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

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.vector.ValueVector;


public interface VectorWrapper<T extends ValueVector> {

  public Class<T> getVectorClass();
  public MaterializedField getField();
  public T getValueVector();
  public T[] getValueVectors();
  public boolean isHyper();
  public void clear();
  public VectorWrapper<T> cloneAndTransfer(BufferAllocator allocator);
  public VectorWrapper<?> getChildWrapper(int[] ids);
  public void transfer(VectorWrapper<?> destination);

  /**
   * Traverse the object graph and determine whether the provided SchemaPath matches data within the Wrapper.  If so, return a TypedFieldId associated with this path.
   * @return TypedFieldId
   */
  public TypedFieldId getFieldIdIfMatches(int id, SchemaPath expectedPath);
}

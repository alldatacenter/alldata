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

import io.netty.buffer.DrillBuf;

import java.util.Iterator;
import java.util.Map;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.util.CallBack;
import org.apache.drill.exec.vector.ValueVector;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestOutputMutator implements OutputMutator, Iterable<VectorWrapper<?>> {
  static final Logger logger = LoggerFactory.getLogger(TestOutputMutator.class);

  private final VectorContainer container = new VectorContainer();
  private final Map<MaterializedField, ValueVector> fieldVectorMap = Maps.newHashMap();
  private final BufferAllocator allocator;

  public TestOutputMutator(BufferAllocator allocator) {
    this.allocator = allocator;
  }

  public void removeField(MaterializedField field) throws SchemaChangeException {
    ValueVector vector = fieldVectorMap.remove(field);
    if (vector == null) {
      throw new SchemaChangeException("Failure attempting to remove an unknown field.");
    }
    container.remove(vector);
    vector.close();
  }

  public void addField(ValueVector vector) {
    container.add(vector);
    fieldVectorMap.put(vector.getField(), vector);
  }

  @Override
  public Iterator<VectorWrapper<?>> iterator() {
    return container.iterator();
  }

  @Override
  public void clear() {

  }

  @Override
  public boolean isNewSchema() {
    return false;
  }

  @Override
  public void allocate(int recordCount) {
    return;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends ValueVector> T addField(MaterializedField field, Class<T> clazz) throws SchemaChangeException {
    ValueVector v = TypeHelper.getNewVector(field, allocator);
    if (!clazz.isAssignableFrom(v.getClass())) {
      throw new SchemaChangeException(String.format("The class that was provided %s does not correspond to the expected vector type of %s.", clazz.getSimpleName(), v.getClass().getSimpleName()));
    }
    addField(v);
    return (T) v;
  }

  @Override
  public DrillBuf getManagedBuffer() {
    return allocator.buffer(255);
  }

  @Override
  public CallBack getCallBack() {
    return null;
  }

  public VectorContainer getContainer() {
    return container;
  }

}

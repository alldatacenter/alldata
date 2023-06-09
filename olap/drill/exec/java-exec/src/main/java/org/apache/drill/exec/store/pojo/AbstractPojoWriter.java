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
package org.apache.drill.exec.store.pojo;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.vector.ValueVector;

/**
 * Parent class for all pojo writers created for each field.
 * Contains common logic for initializing value vector, stores field name and its type.
 */
public abstract class AbstractPojoWriter<V extends ValueVector> implements PojoWriter {

  protected V vector;
  private final String fieldName;
  private final MajorType type;

  public AbstractPojoWriter(String fieldName, MajorType type) {
    this.fieldName = fieldName;
    this.type = type;
  }

  @Override
  public void init(OutputMutator output) throws SchemaChangeException {
    MaterializedField mf = MaterializedField.create(fieldName, type);
    @SuppressWarnings("unchecked")
    Class<V> valueVectorClass = (Class<V>) TypeHelper.getValueVectorClass(type.getMinorType(), type.getMode());
    this.vector = output.addField(mf, valueVectorClass);
  }

  @Override
  public void allocate() {
    vector.allocateNew();
  }

  @Override
  public void setValueCount(int valueCount) {
    vector.getMutator().setValueCount(valueCount);
  }

  @Override
  public void cleanup() {
  }

}
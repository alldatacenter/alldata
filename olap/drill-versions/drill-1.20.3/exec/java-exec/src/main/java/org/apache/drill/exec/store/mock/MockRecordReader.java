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
package org.apache.drill.exec.store.mock;

import java.util.Map;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.store.AbstractRecordReader;
import org.apache.drill.exec.store.mock.MockTableDef.MockColumn;
import org.apache.drill.exec.store.mock.MockTableDef.MockScanEntry;
import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.ValueVector;

public class MockRecordReader extends AbstractRecordReader {
  private final MockScanEntry config;
  private final FragmentContext context;
  private ValueVector[] valueVectors;
  private int recordsRead;
  private int batchRecordCount;
  @SuppressWarnings("unused")
  private OperatorContext operatorContext;

  public MockRecordReader(FragmentContext context, MockScanEntry config) {
    this.context = context;
    this.config = config;
  }

  private int getEstimatedRecordSize(MockColumn[] types) {
    if (types == null) {
      return 0;
    }

    int x = 0;
    for (int i = 0; i < types.length; i++) {
      x += TypeHelper.getSize(types[i].getMajorType());
    }
    return x;
  }

  private MaterializedField getVector(String name, MajorType type) {
    assert context != null : "Context shouldn't be null.";
    final MaterializedField f = MaterializedField.create(name, type);
    return f;
  }

  @Override
  public void setup(OperatorContext context, OutputMutator output) throws ExecutionSetupException {
    try {
      final int estimateRowSize = getEstimatedRecordSize(config.getTypes());
      if (config.getTypes() == null) {
        return;
      }
      valueVectors = new ValueVector[config.getTypes().length];
      batchRecordCount = 250000 / estimateRowSize;

      for (int i = 0; i < config.getTypes().length; i++) {
        final MajorType type = config.getTypes()[i].getMajorType();
        final MaterializedField field = getVector(config.getTypes()[i].getName(), type);
        final Class<? extends ValueVector> vvClass = TypeHelper.getValueVectorClass(field.getType().getMinorType(), field.getDataMode());
        valueVectors[i] = output.addField(field, vvClass);
      }
    } catch (SchemaChangeException e) {
      throw new ExecutionSetupException("Failure while setting up fields", e);
    }
  }

  @Override
  public int next() {
    if (recordsRead >= this.config.getRecords()) {
      return 0;
    }

    final int recordSetSize = Math.min(batchRecordCount, this.config.getRecords() - recordsRead);
    recordsRead += recordSetSize;

    if (valueVectors == null) {
      return recordSetSize;
    }

    for (final ValueVector v : valueVectors) {
      final ValueVector.Mutator m = v.getMutator();
      m.generateTestData(recordSetSize);
    }

    return recordSetSize;
  }

  @Override
  public void allocate(Map<String, ValueVector> vectorMap) throws OutOfMemoryException {
    try {
      for (final ValueVector v : vectorMap.values()) {
        AllocationHelper.allocate(v, Character.MAX_VALUE, 50, 10);
      }
    } catch (NullPointerException e) {
      throw new OutOfMemoryException();
    }
  }

  @Override
  public void close() { }
}

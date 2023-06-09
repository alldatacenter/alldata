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
package org.apache.drill.exec.physical.resultSet.model.single;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.model.MetadataProvider;
import org.apache.drill.exec.physical.resultSet.model.MetadataProvider.MetadataCreator;
import org.apache.drill.exec.physical.resultSet.model.MetadataProvider.MetadataRetrieval;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.AbstractMapVector;
import org.apache.drill.exec.vector.complex.RepeatedDictVector;
import org.apache.drill.exec.vector.complex.RepeatedMapVector;

/**
 * Given a vector container, and a metadata schema that matches the container,
 * walk the schema tree to allocate new vectors according to a given
 * row count and the size information provided in column metadata.
 * <p>
 *   {@link org.apache.drill.exec.vector.AllocationHelper} - the class which this one replaces
 *   {@link org.apache.drill.exec.record.VectorInitializer} - an earlier cut at implementation
 *   based on data from the {@link org.apache.drill.exec.record.RecordBatchSizer}
 * </p>
 */

// TODO: Does not yet handle lists; lists are a simple extension
// of the array-handling logic below.

public class VectorAllocator {

  private final VectorContainer container;

  public VectorAllocator(VectorContainer container) {
    this.container = container;
  }

  public void allocate(int rowCount) {
    allocate(rowCount, new MetadataCreator());
  }

  public void allocate(int rowCount, TupleMetadata schema) {
    allocate(rowCount, new MetadataRetrieval(schema));
  }

  public void allocate(int rowCount, MetadataProvider mdProvider) {
    for (int i = 0; i < container.getNumberOfColumns(); i++) {
      final ValueVector vector = container.getValueVector(i).getValueVector();
      allocateVector(vector, mdProvider.metadata(i, vector.getField()), rowCount, mdProvider);
    }
  }

  private void allocateVector(ValueVector vector, ColumnMetadata metadata, int valueCount, MetadataProvider mdProvider) {
    final MajorType type = vector.getField().getType();
    assert vector.getField().getName().equals(metadata.name());
    assert type.getMinorType() == metadata.type();
    if (type.getMinorType() == MinorType.MAP) {
      if (type.getMode() == DataMode.REPEATED) {
        allocateMapArray((RepeatedMapVector) vector, metadata, valueCount, mdProvider);
      } else {
        allocateMap((AbstractMapVector) vector, metadata, valueCount, mdProvider);
      }
    } else if (type.getMinorType() == MinorType.DICT) {
      if (type.getMode() == DataMode.REPEATED) {
        allocateDictArray((RepeatedDictVector) vector, metadata, valueCount, mdProvider);
      } else {
        allocateMap((AbstractMapVector) vector, metadata, valueCount, mdProvider);
      }
    } else {
      allocatePrimitive(vector, metadata, valueCount);
    }
  }

  private void allocatePrimitive(ValueVector vector,
      ColumnMetadata metadata, int valueCount) {
    AllocationHelper.allocatePrecomputedChildCount(vector,
        valueCount,
        metadata.expectedWidth(),
        metadata.expectedElementCount());
  }

  private void allocateMapArray(RepeatedMapVector vector,
      ColumnMetadata metadata, int valueCount, MetadataProvider mdProvider) {
    vector.getOffsetVector().allocateNew(valueCount);
    final int expectedValueCount = valueCount * metadata.expectedElementCount();
    allocateMap(vector, metadata, expectedValueCount, mdProvider);
  }

  private void allocateDictArray(RepeatedDictVector vector, ColumnMetadata metadata,
                                 int valueCount, MetadataProvider mdProvider) {
    vector.getOffsetVector().allocateNew(valueCount);
    final int expectedValueCount = valueCount * metadata.expectedElementCount();
    allocateMap((AbstractMapVector) vector.getDataVector(), metadata, expectedValueCount, mdProvider);
  }

  private void allocateMap(AbstractMapVector vector, ColumnMetadata metadata, int valueCount, MetadataProvider mdProvider) {
    final MetadataProvider mapProvider = mdProvider.childProvider(metadata);
    final TupleMetadata mapSchema = metadata.tupleSchema();
    assert mapSchema != null;
    int i = 0;
    for (final ValueVector child : vector) {
      allocateVector(child, mapProvider.metadata(i, child.getField()), valueCount, mapProvider);
      i++;
    }
  }
}

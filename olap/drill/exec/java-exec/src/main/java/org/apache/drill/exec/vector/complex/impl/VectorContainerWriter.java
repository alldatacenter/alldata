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
package org.apache.drill.exec.vector.complex.impl;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.util.CallBack;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.MapVector;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ComplexWriter;

public class VectorContainerWriter extends AbstractFieldWriter implements ComplexWriter {
  //private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VectorContainerWriter.class);

  private final SingleMapWriter mapRoot;
  private final SpecialMapVector mapVector;
  private final OutputMutator mutator;

  public VectorContainerWriter(OutputMutator mutator, boolean unionEnabled) {
    super(null);
    this.mutator = mutator;
    mapVector = new SpecialMapVector(mutator.getCallBack());
    mapRoot = new SingleMapWriter(mapVector, this, unionEnabled);
  }

  public VectorContainerWriter(OutputMutator mutator) {
    this(mutator, false);
  }

  @Override
  public MaterializedField getField() {
    return mapVector.getField();
  }

  @Override
  public int getValueCapacity() {
    return mapRoot.getValueCapacity();
  }

  public MapVector getMapVector() {
    return mapVector;
  }

  @Override
  public void reset() {
    setPosition(0);
  }

  @Override
  public void close() throws Exception {
    clear();
    mapRoot.close();
    mapVector.close();
  }

  @Override
  public void clear() {
    mapRoot.clear();
  }

  public SingleMapWriter getWriter() {
    return mapRoot;
  }

  @Override
  public void setValueCount(int count) {
    mapRoot.setValueCount(count);
  }

  @Override
  public void setPosition(int index) {
    super.setPosition(index);
    mapRoot.setPosition(index);
  }

  @Override
  public void allocate() {
    mapRoot.allocate();
  }

  private class SpecialMapVector extends MapVector {

    public SpecialMapVector(CallBack callback) {
      super("", null, callback);
    }

    @Override
    public <T extends ValueVector> T addOrGet(String name, MajorType type, Class<T> clazz) {
      try {
        final ValueVector v = mutator.addField(MaterializedField.create(name, type), clazz);
        putChild(name, v);
        return this.typeify(v, clazz);
      } catch (SchemaChangeException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  @Override
  public MapWriter rootAsMap() {
    return mapRoot;
  }

  @Override
  public ListWriter rootAsList() {
    throw new UnsupportedOperationException(
        "Drill doesn't support objects whose first level is a scalar or array.  Objects must start as maps.");
  }
}

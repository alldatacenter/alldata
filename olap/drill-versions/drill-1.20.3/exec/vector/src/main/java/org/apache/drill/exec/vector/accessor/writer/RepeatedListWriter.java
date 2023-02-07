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
package org.apache.drill.exec.vector.accessor.writer;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.complex.RepeatedListVector;

/**
 * Implements a writer for a repeated list. A repeated list is really
 * an array of arrays. It is an "array" because it does not support
 * nulls. (The Drill [non-repeated] "list vector" does support nulls.)
 * The terminology is confusing, but it is what Drill has settled upon.
 * <p>
 * Because repeated lists can be nested, they support incremental
 * construction. Build the outer array, then the inner, then the leaf
 * to build a 3D array. Since building is incremental, this form of
 * array must track state and use the state to keep the newly-added
 * element writer in sync.
 * <p>
 * To keep things (relatively) simple, the repeated list array starts
 * out with an inner list. For the row set writer, the inner list
 * is the actual element writer. For the result set loader, the inner
 * list is a dummy, to be replaced by the real one once it is discovered
 * by reading data (or by parsing a schema.)
 */

public class RepeatedListWriter extends ObjectArrayWriter {

  public interface ArrayListener {

    AbstractObjectWriter setChild(ArrayWriter array, ColumnMetadata column);

    AbstractObjectWriter setChild(ArrayWriter array, MaterializedField field);
  }

  private State state = State.IDLE;
  private ArrayListener listener;

  protected RepeatedListWriter(ColumnMetadata schema, UInt4Vector offsetVector,
      AbstractObjectWriter elementWriter) {
    super(schema, offsetVector, elementWriter);
  }

  public static AbstractObjectWriter buildRepeatedList(ColumnMetadata schema,
      RepeatedListVector vector, AbstractObjectWriter elementWriter) {
    AbstractArrayWriter arrayWriter = new RepeatedListWriter(schema,
        vector.getOffsetVector(),
        elementWriter);
    return new ArrayObjectWriter(arrayWriter);
  }

  public void bindListener(ArrayListener listener) {
    this.listener = listener;
  }

  public AbstractObjectWriter defineElement(MaterializedField schema) {
    if (listener == null || elementObjWriter.schema().type() != MinorType.NULL) {
      throw new UnsupportedOperationException();
    } else {
      return replaceChild(listener.setChild(this, schema));
    }
  }

  public AbstractObjectWriter defineElement(ColumnMetadata schema) {
    if (listener == null || elementObjWriter.schema().type() != MinorType.NULL) {
      throw new UnsupportedOperationException();
    } else {
      return replaceChild(listener.setChild(this, schema));
    }
  }

  private AbstractObjectWriter replaceChild(AbstractObjectWriter newChild) {
    elementObjWriter = newChild;
    elementObjWriter.events().bindIndex(elementIndex);
    if (state != State.IDLE) {
      elementObjWriter.events().startWrite();
      if (state == State.IN_ROW) {
        elementObjWriter.events().startRow();
      }
    }
    return elementObjWriter;
  }

  @Override
  public void startWrite() {
    assert state == State.IDLE;
    state = State.IN_WRITE;
    super.startWrite();
  }

  @Override
  public void startRow() {
    assert state == State.IN_WRITE;
    state = State.IN_ROW;
    super.startRow();
  }

  @Override
  public void saveRow() {
    assert state == State.IN_ROW;
    super.saveRow();
    state = State.IN_WRITE;
  }

  @Override
  public void endWrite() {
    assert state != State.IDLE;
    super.endWrite();
    state = State.IDLE;
  }
}

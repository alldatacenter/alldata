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
package org.apache.drill.exec.vector.accessor.writer.dummy;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.accessor.ColumnReader;
import org.apache.drill.exec.vector.accessor.ColumnWriterIndex;
import org.apache.drill.exec.vector.accessor.writer.AbstractArrayWriter;
import org.apache.drill.exec.vector.accessor.writer.AbstractObjectWriter;
import org.apache.drill.exec.vector.accessor.writer.OffsetVectorWriter;

/**
 * Dummy scalar array writer that allows a client to write values into
 * the array, but discards all of them. Provides no implementations of
 * any methods, all are simply ignored.
 * <p>
 * Experience may suggest that some methods must return non-dummy
 * values, such as the number of items in the array. That can be added
 * as needed.
 */
public class DummyArrayWriter extends AbstractArrayWriter {

  public static class DummyOffsetVectorWriter extends DummyScalarWriter implements OffsetVectorWriter {

    public DummyOffsetVectorWriter() {
      super(null);
    }

    @Override
    public int rowStartOffset() { return 0; }

    @Override
    public int nextOffset() { return 0; }

    @Override
    public void setNextOffset(int vectorIndex) { }
  }

  public static final DummyOffsetVectorWriter offsetVectorWriter = new DummyOffsetVectorWriter();

  public DummyArrayWriter(
      ColumnMetadata schema,
      AbstractObjectWriter elementWriter) {
    super(schema, elementWriter, offsetVectorWriter);
  }

  @Override
  public void save() { }

  @Override
  public void setObject(Object array) { }

  @Override
  public void startWrite() { }

  @Override
  public void startRow() { }

  @Override
  public void endArrayValue() { }

  @Override
  public void restartRow() { }

  @Override
  public void saveRow() { }

  @Override
  public void endWrite() { }

  @Override
  public void preRollover() { }

  @Override
  public void postRollover() { }

  @Override
  public int lastWriteIndex() { return 0; }

  @Override
  public void bindIndex(ColumnWriterIndex index) { }

  @Override
  public boolean isProjected() { return false; }

  @Override
  public void copy(ColumnReader from) { }
}

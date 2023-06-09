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
import org.apache.drill.exec.vector.accessor.DictWriter;
import org.apache.drill.exec.vector.accessor.ObjectType;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.ValueType;
import org.apache.drill.exec.vector.accessor.writer.DictEntryWriter;
import org.apache.drill.exec.vector.accessor.writer.ObjectDictWriter;

public class DummyDictWriter extends DummyArrayWriter implements DictWriter {

  public DummyDictWriter(ColumnMetadata schema, DictEntryWriter.DictEntryObjectWriter entryObjectWriter) {
    super(schema, entryObjectWriter);
  }

  @Override
  public ValueType keyType() {
    return tuple().scalar(ObjectDictWriter.FIELD_KEY_ORDINAL).valueType();
  }

  @Override
  public ObjectType valueType() {
    return tuple().type(ObjectDictWriter.FIELD_VALUE_ORDINAL);
  }

  @Override
  public ScalarWriter keyWriter() {
    return tuple().scalar(ObjectDictWriter.FIELD_KEY_ORDINAL);
  }

  @Override
  public ObjectWriter valueWriter() {
    return tuple().column(ObjectDictWriter.FIELD_VALUE_ORDINAL);
  }
}

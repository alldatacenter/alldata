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
package org.apache.drill.exec.store.mapr.db.json;

import org.apache.drill.exec.vector.complex.impl.MapOrListWriterImpl;
import org.ojai.DocumentReader;

import io.netty.buffer.DrillBuf;

public class NumbersAsDoubleValueWriter extends OjaiValueWriter {

  public NumbersAsDoubleValueWriter(DrillBuf buffer) {
    super(buffer);
  }

  protected void writeFloat(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
    writer.float8(fieldName).writeFloat8(reader.getFloat());
  }

  protected void writeLong(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
      writer.float8(fieldName).writeFloat8(reader.getLong());
  }

  protected void writeInt(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
      writer.float8(fieldName).writeFloat8(reader.getInt());
  }

  protected void writeShort(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
      writer.float8(fieldName).writeFloat8(reader.getShort());
  }

  protected void writeByte(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
      writer.float8(fieldName).writeFloat8(reader.getByte());
  }

}

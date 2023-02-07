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

import java.nio.ByteBuffer;

import org.apache.drill.exec.vector.complex.impl.MapOrListWriterImpl;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.MapOrListWriter;
import org.ojai.DocumentReader;

import com.mapr.org.apache.hadoop.hbase.util.Bytes;

import io.netty.buffer.DrillBuf;

public class AllTextValueWriter extends OjaiValueWriter {

  public AllTextValueWriter(DrillBuf buffer) {
    super(buffer);
  }

  protected void writeTimeStamp(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
    writeString(writer, fieldName, reader.getTimestamp().toUTCString());
  }

  protected void writeTime(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
    writeString(writer, fieldName, reader.getTime().toTimeStr());
  }

  protected void writeDate(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
    writeString(writer, fieldName, reader.getDate().toDateStr());
  }

  protected void writeDouble(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
    writeString(writer, fieldName, String.valueOf(reader.getDouble()));
  }

  protected void writeFloat(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
    writeString(writer, fieldName, String.valueOf(reader.getFloat()));
  }

  protected void writeLong(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
    writeString(writer, fieldName, String.valueOf(reader.getLong()));
  }

  protected void writeInt(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
    writeString(writer, fieldName, String.valueOf(reader.getInt()));
  }

  protected void writeShort(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
    writeString(writer, fieldName, String.valueOf(reader.getShort()));
  }

  protected void writeByte(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
    writeString(writer, fieldName, String.valueOf(reader.getByte()));
  }

  protected void writeBoolean(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
    writeString(writer, fieldName, String.valueOf(reader.getBoolean()));
  }

  protected void writeBinary(MapOrListWriter writer, String fieldName, ByteBuffer buf) {
    writeString(writer, fieldName, Bytes.toString(buf));
  }

}

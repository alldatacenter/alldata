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
package org.apache.drill.exec.physical.rowSet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.physical.impl.protocol.BatchAccessor;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.TupleMetadata;

/**
 * Helper class to obtain string representation of RowSet.
 * Example of the output:
 * <pre>
 *   #: `id` INT NOT NULL, `mapCol` STRUCT<`a` INT, `b` VARCHAR>, `arrayInt` ARRAY<INT>
 *   0: 1, {20, "say"}, [1, 1, 2]
 *   1: 2, {20, "hello"}, []
 *   2: 2, {20, null}, [3, 5, 8]
 * </pre>
 */
public class RowSetFormatter {
  private final RowSet rowSet;

  private final Writer writer;

  public RowSetFormatter(RowSet rowSet, Writer writer) {
    this.rowSet = rowSet;
    this.writer = writer;
  }

  public static void print(RowSet rowSet) {
    new RowSetFormatter(rowSet, new OutputStreamWriter(System.out)).write();
  }

  public static void print(VectorContainer container) {
    RowSets.wrap(container).print();
  }

  public static void print(BatchAccessor batch) {
    RowSets.wrap(batch).print();
  }

  public static String toString(RowSet rowSet) {
    StringBuilderWriter out = new StringBuilderWriter();
    new RowSetFormatter(rowSet, out).write();
    return out.toString();
  }

  public void write() {
    try {
      SelectionVectorMode selectionMode = rowSet.indirectionType();
      RowSetReader reader = rowSet.reader();
      int colCount = reader.tupleSchema().size();
      writeSchema(writer, selectionMode, reader);
      while (reader.next()) {
        writeHeader(writer, reader, selectionMode);
        for (int i = 0; i < colCount; i++) {
          if (i > 0) {
            writer.write(", ");
          }
          writer.write(reader.column(i).getAsString());
        }
        writer.write("\n");
      }
      writer.flush();
    } catch (IOException e) {
      throw new DrillRuntimeException("Error happened when writing rowSet to writer", e);
    }
  }

  private void writeSchema(Writer writer, SelectionVectorMode selectionMode, RowSetReader reader) throws IOException {
    writer.write("#");
    switch (selectionMode) {
      case FOUR_BYTE:
        writer.write(" (batch #, row #)");
        break;
      case TWO_BYTE:
        writer.write(" (row #)");
        break;
      default:
        break;
    }
    writer.write(": ");
    TupleMetadata schema = reader.tupleSchema();
    writeTupleSchema(writer, schema);
    writer.write("\n");
  }

  private void writeTupleSchema(Writer writer, TupleMetadata schema) throws IOException {
    for (int i = 0; i < schema.size(); i++) {
      if (i > 0) {
        writer.write(", ");
      }
      writer.write(schema.metadata(i).columnString());
    }
  }

  private void writeHeader(Writer writer, RowSetReader reader, SelectionVectorMode selectionMode) throws IOException {
    writer.write(String.valueOf(reader.logicalIndex()));
    switch (selectionMode) {
      case FOUR_BYTE:
        writer.write(" (");
        writer.write(String.valueOf(reader.hyperVectorIndex()));
        writer.write(", ");
        writer.write(String.valueOf(reader.offset()));
        writer.write(")");
        break;
      case TWO_BYTE:
        writer.write(" (");
        writer.write(String.valueOf(reader.offset()));
        writer.write(")");
        break;
      default:
        break;
    }
    writer.write(": ");
  }
}

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
package org.apache.drill.exec.store.parquet.decimal;

import io.netty.buffer.DrillBuf;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.PrimitiveType;

public abstract class DecimalValueWriter {

  /**
   * Takes value from specified {@code DrillBuf buffer}, converts it into required format
   * if needed and passes the value into specified {@code RecordConsumer consumer}.
   *
   * @param consumer  abstraction for writing records
   * @param buffer    the source of data that should be written
   * @param start     start position of data in buffer
   * @param end       end position of data in buffer
   * @param precision precision of the value that should be stored
   */
  public abstract void writeValue(RecordConsumer consumer, DrillBuf buffer,
      int start, int end, int precision);

  /**
   * Creates and returns writer suitable for specified {@code PrimitiveType.PrimitiveTypeName type}.
   *
   * @param type the type of the value in output file.
   * @return writer for decimal values.
   */
  public static DecimalValueWriter getDecimalValueWriterForType(
      PrimitiveType.PrimitiveTypeName type) {
    switch (type) {
      case INT32:
        return new Int32DecimalParquetValueWriter();
      case INT64:
        return new Int64DecimalParquetValueWriter();
      case FIXED_LEN_BYTE_ARRAY:
        return new FixedLenDecimalParquetValueWriter();
      case BINARY:
        return new BinaryDecimalParquetValueWriter();
      default:
        throw new UnsupportedOperationException(
            String.format("Specified PrimitiveTypeName %s cannot be used to store decimal values", type));
    }
  }
}

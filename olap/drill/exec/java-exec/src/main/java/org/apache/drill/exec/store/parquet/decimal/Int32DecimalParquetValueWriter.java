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

import org.apache.drill.shaded.guava.com.google.common.primitives.Ints;
import io.netty.buffer.DrillBuf;
import org.apache.parquet.io.api.RecordConsumer;

import java.util.Arrays;

/**
 * Parquet value writer for passing decimal values
 * into {@code RecordConsumer} to be stored as INT32 type.
 */
public class Int32DecimalParquetValueWriter extends DecimalValueWriter {

  @Override
  public void writeValue(RecordConsumer consumer, DrillBuf buffer, int start, int end, int precision) {
    byte[] output;
    int startPos;
    int length = end - start;
    startPos = Ints.BYTES - length;
    output = new byte[Ints.BYTES];
    if (startPos >= 0) {
      buffer.getBytes(start, output, startPos, length);
    } else {
      // in this case value from FIXED_LEN_BYTE_ARRAY or BINARY field was taken, ignore leading bytes
      buffer.getBytes(start - startPos, output, 0, length + startPos);
      startPos = 0;
    }
    if (output[startPos] < 0) {
      Arrays.fill(output, 0, output.length - length, (byte) -1);
    }
    consumer.addInteger(Ints.fromByteArray(output));
  }
}

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

package org.apache.drill.exec.udfs;

import io.netty.buffer.DrillBuf;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;

import java.util.Iterator;

public class ComplexSchemaUtils {

  public static DrillBuf getFields(FieldReader reader, BaseWriter.ComplexWriter outWriter, DrillBuf buffer) {

    BaseWriter.MapWriter queryMapWriter = outWriter.rootAsMap();

    if (reader.getType().getMinorType() != MinorType.MAP) {
      // If the field is not a map, return an empty map
      queryMapWriter.start();
      queryMapWriter.end();
    }

    Iterator<String> fieldIterator = reader.iterator();
    queryMapWriter.start();

    while (fieldIterator.hasNext()) {
      String fieldName = fieldIterator.next();
      FieldReader fieldReader = reader.reader(fieldName);
      String dataType = fieldReader.getType().getMinorType().toString();

      DataMode dataMode = fieldReader.getType().getMode();
      if (dataMode == DataMode.REPEATED) {
        dataType = dataMode + "_" + dataType;
      }

      VarCharHolder rowHolder = new VarCharHolder();
      byte[] rowStringBytes = dataType.getBytes();
      buffer = buffer.reallocIfNeeded(rowStringBytes.length);
      buffer.setBytes(0, rowStringBytes);

      rowHolder.start = 0;
      rowHolder.end = rowStringBytes.length;
      rowHolder.buffer = buffer;

      queryMapWriter.varChar(fieldName).write(rowHolder);
    }
    queryMapWriter.end();
    return buffer;
  }
}

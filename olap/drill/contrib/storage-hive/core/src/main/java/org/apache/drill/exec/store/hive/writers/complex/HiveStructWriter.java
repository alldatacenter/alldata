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
package org.apache.drill.exec.store.hive.writers.complex;

import org.apache.drill.exec.store.hive.writers.HiveValueWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;

public class HiveStructWriter implements HiveValueWriter {

  private final StructObjectInspector structObjectInspector;

  private final StructField[] structFields;

  private final HiveValueWriter[] fieldWriters;

  private final BaseWriter.MapWriter structWriter;

  public HiveStructWriter(StructObjectInspector structObjectInspector, StructField[] structFields, HiveValueWriter[] fieldWriters, BaseWriter.MapWriter structWriter) {
    this.structObjectInspector = structObjectInspector;
    this.structFields = structFields;
    this.fieldWriters = fieldWriters;
    this.structWriter = structWriter;
  }

  @Override
  public void write(Object value) {
    structWriter.start();
    for (int fieldIdx = 0; fieldIdx < structFields.length; fieldIdx++) {
      Object fieldValue = structObjectInspector.getStructFieldData(value, structFields[fieldIdx]);
      if (fieldValue == null) {
        throw new UnsupportedOperationException("Null is not supported in Hive struct");
      } else {
        fieldWriters[fieldIdx].write(fieldValue);
      }
    }
    structWriter.end();
  }

}

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

import java.util.List;

import org.apache.drill.exec.store.hive.writers.HiveValueWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;
import org.apache.hadoop.hive.serde2.objectinspector.ListObjectInspector;

public class HiveListWriter implements HiveValueWriter {

  private final ListObjectInspector listInspector;

  private final HiveValueWriter elementWriter;

  private final BaseWriter.ListWriter listWriter;

  public HiveListWriter(ListObjectInspector listInspector, BaseWriter.ListWriter listWriter, HiveValueWriter elementWriter) {
    this.listInspector = listInspector;
    this.elementWriter = elementWriter;
    this.listWriter = listWriter;
  }


  @Override
  public void write(Object value) {
    List<?> list = listInspector.getList(value);
    if (list == null) {
      throw new UnsupportedOperationException("Null array is not supported in Hive.");
    }
    listWriter.startList();
    for (final Object element : list) {
      if (element == null) {
        throw new UnsupportedOperationException("Null is not supported in Hive array!");
      } else {
        elementWriter.write(element);
      }
    }
    listWriter.endList();
  }

}

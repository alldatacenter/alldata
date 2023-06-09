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
import org.apache.hadoop.hive.serde2.objectinspector.UnionObjectInspector;

public class HiveUnionWriter implements HiveValueWriter {

  private final HiveValueWriter[] unionFieldWriters;

  private final UnionObjectInspector unionObjectInspector;

  public HiveUnionWriter(HiveValueWriter[] unionFieldWriters, UnionObjectInspector unionObjectInspector) {
    this.unionFieldWriters = unionFieldWriters;
    this.unionObjectInspector = unionObjectInspector;
  }

  @Override
  public void write(Object value) {
    int tag = unionObjectInspector.getTag(value);
    Object field = unionObjectInspector.getField(value);
    if (field == null) {
      throw new UnsupportedOperationException("Null value is not supported in Hive union.");
    } else {
      unionFieldWriters[tag].write(field);
    }
  }
}

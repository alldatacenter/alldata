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

import java.util.Map;

import org.apache.drill.exec.store.hive.writers.HiveValueWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;
import org.apache.hadoop.hive.serde2.objectinspector.MapObjectInspector;

public class HiveMapWriter implements HiveValueWriter {

  private final MapObjectInspector mapInspector;
  private final BaseWriter.DictWriter dictWriter;
  private final HiveValueWriter keyWriter;
  private final HiveValueWriter valueWriter;


  public HiveMapWriter(MapObjectInspector mapInspector, BaseWriter.DictWriter dictWriter,
                       HiveValueWriter keyWriter, HiveValueWriter valueWriter) {
    this.mapInspector = mapInspector;
    this.dictWriter = dictWriter;
    this.keyWriter = keyWriter;
    this.valueWriter = valueWriter;
  }

  @Override
  public void write(Object mapHolder) {
    Map<?, ?> map = mapInspector.getMap(mapHolder);
    if (map == null) {
      throw new UnsupportedOperationException("Null map is not supported in Hive.");
    }
    dictWriter.start();
    map.forEach((key, value) -> {
      if (key == null || value == null) {
        throw new UnsupportedOperationException("Null key or value are not supported in Hive maps.");
      }
      dictWriter.startKeyValuePair();
      keyWriter.write(key);
      valueWriter.write(value);
      dictWriter.endKeyValuePair();
    });
    dictWriter.end();
  }

}

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
package org.apache.drill.exec.store.hive.writers.primitive;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.apache.drill.exec.vector.complex.writer.VarDecimalWriter;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.HiveDecimalObjectInspector;

public class HiveDecimalWriter extends AbstractSingleValueWriter<HiveDecimalObjectInspector, VarDecimalWriter> {

  private final int scale;

  public HiveDecimalWriter(HiveDecimalObjectInspector inspector, VarDecimalWriter writer, int scale) {
    super(inspector, writer);
    this.scale = scale;
  }

  @Override
  public void write(Object value) {
    BigDecimal decimalValue = inspector.getPrimitiveJavaObject(value).bigDecimalValue()
        .setScale(scale, RoundingMode.HALF_UP);
    writer.writeVarDecimal(decimalValue);
  }

}

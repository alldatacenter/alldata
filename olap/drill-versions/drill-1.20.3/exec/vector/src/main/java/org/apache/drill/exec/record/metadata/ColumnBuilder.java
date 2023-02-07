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
package org.apache.drill.exec.record.metadata;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.MaterializedField;

/**
 * Build a column schema (AKA "materialized field") based on name and a
 * variety of schema options. Every column needs a name and (minor) type,
 * some may need a mode other than required, may need a width, may
 * need scale and precision, and so on.
 */
public class ColumnBuilder {

  private final String name;
  private final MajorType.Builder typeBuilder;

  public ColumnBuilder(String name, MinorType type) {
    this.name = name;
    this.typeBuilder = MajorType.newBuilder()
      .setMinorType(type)
      .setMode(DataMode.REQUIRED);
  }

  public ColumnBuilder setMode(DataMode mode) {
    typeBuilder.setMode(mode);
    return this;
  }

  public ColumnBuilder setWidth(int width) {
    return setPrecision(width);
  }

  public ColumnBuilder setPrecision(int precision) {

    // Set the precision only if non-zero. Some (naive) code in Drill
    // checks if precision is set as a way to determine if the precision
    // is non-zero. The correct pattern is to check if the precision is
    // non-zero. This unnecessary check exists simply to avoid breaking
    // that incorrect code.

    if (precision != 0) {
      typeBuilder.setPrecision(precision);
    }
    return this;
  }

  public ColumnBuilder setPrecisionAndScale(int precision, int scale) {
    if (precision != 0) {
      typeBuilder.setPrecision(precision);
      typeBuilder.setScale(scale);
    }
    return this;
  }

  public MaterializedField build() {
    return MaterializedField.create(name, typeBuilder.build());
  }
}

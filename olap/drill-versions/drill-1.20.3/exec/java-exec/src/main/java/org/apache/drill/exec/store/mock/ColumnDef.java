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
package org.apache.drill.exec.store.mock;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.store.mock.MockTableDef.MockColumn;

/**
 * Defines a column for the "enhanced" version of the mock data
 * source. This class is built from the column definitions in either
 * the physical plan or an SQL statement (which gives rise to a
 * physical plan.)
 */

public class ColumnDef {
  public MockColumn mockCol;
  public String name;
  public int width;
  public FieldGen generator;
  public boolean nullable;
  public int nullablePercent;

  public ColumnDef(MockColumn mockCol) {
    this.mockCol = mockCol;
    name = mockCol.getName();
    if (mockCol.getMinorType() == MinorType.VARCHAR &&
        mockCol.getWidth() > 0) {
      width = mockCol.getWidth();
    } else {
      width = TypeHelper.getSize(mockCol.getMajorType());
    }
    nullable = mockCol.getMode() == DataMode.OPTIONAL;
    if (nullable) {
      nullablePercent = 25;
      if (mockCol.properties != null) {
        Object value = mockCol.properties.get("nulls");
        if (value != null  && value instanceof Integer) {
          nullablePercent = (Integer) value;
        }
      }
    }
    makeGenerator();
  }

  /**
   * Create the data generator class for this column. The generator is
   * created to match the data type by default. Or, the plan can
   * specify a generator class (in which case the plan must ensure that
   * the generator produces the correct value for the column data type.)
   * The generator names a class: either a fully qualified name, or a
   * class in this package.
   */

  private void makeGenerator() {
    String genName = mockCol.getGenerator();
    if (genName != null) {
      if (! genName.contains(".")) {
        genName = "org.apache.drill.exec.store.mock." + genName;
      }
      try {
        ClassLoader cl = getClass().getClassLoader();
        Class<?> genClass = cl.loadClass(genName);
        generator = (FieldGen) genClass.newInstance();
      } catch (ClassNotFoundException | InstantiationException
          | IllegalAccessException | ClassCastException e) {
        throw new IllegalArgumentException("Generator " + genName + " is undefined for mock field " + name);
      }
      return;
    }

    makeDefaultGenerator();
  }

  private void makeDefaultGenerator() {

    MinorType minorType = mockCol.getMinorType();
    switch (minorType) {
    case BIGINT:
      break;
    case BIT:
      generator = new BooleanGen();
      break;
    case DATE:
      break;
    case DECIMAL18:
      break;
    case DECIMAL28DENSE:
      break;
    case DECIMAL28SPARSE:
      break;
    case DECIMAL38DENSE:
      break;
    case DECIMAL38SPARSE:
      break;
    case VARDECIMAL:
      break;
    case DECIMAL9:
      break;
    case FIXED16CHAR:
      break;
    case FIXEDBINARY:
      break;
    case FIXEDCHAR:
      break;
    case FLOAT4:
      break;
    case FLOAT8:
      generator = new DoubleGen();
      break;
    case GENERIC_OBJECT:
      break;
    case INT:
      generator = new IntGen();
      break;
    case INTERVAL:
      break;
    case INTERVALDAY:
      break;
    case INTERVALYEAR:
      break;
    case LATE:
      break;
    case LIST:
      break;
    case MAP:
      break;
    case MONEY:
      break;
    case NULL:
      break;
    case SMALLINT:
      break;
    case TIME:
      break;
    case TIMESTAMP:
      break;
    case TIMESTAMPTZ:
      break;
    case TIMETZ:
      break;
    case TINYINT:
      break;
    case UINT1:
      break;
    case UINT2:
      break;
    case UINT4:
      break;
    case UINT8:
      break;
    case UNION:
      break;
    case VAR16CHAR:
      break;
    case VARBINARY:
      break;
    case VARCHAR:
      generator = new StringGen();
      break;
    default:
      break;
    }
    if (generator == null) {
      throw new IllegalArgumentException("No default column generator for column " + name + " of type " + minorType);
    }
  }

  public ColumnDef(MockColumn mockCol, int rep) {
    this(mockCol);
    name += Integer.toString(rep);
  }

  public MockColumn getConfig() { return mockCol; }
  public String getName() { return name; }
}

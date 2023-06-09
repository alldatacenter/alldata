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

<@pp.dropOutputFile />
<@pp.changeOutputFile name="org/apache/drill/exec/store/parquet/ParquetTypeHelper.java" />
<#include "/@includes/license.ftl" />

package org.apache.drill.exec.store.parquet;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.planner.types.DrillRelDataTypeSystem;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.parquet.schema.OriginalType;
import org.apache.parquet.schema.DecimalMetadata;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type.Repetition;

import java.util.HashMap;
import java.util.Map;

/*
 * This class is generated using freemarker and the ${.template_name} template.
 */

public class ParquetTypeHelper {
  private static Map<MinorType,PrimitiveTypeName> typeMap;
  private static Map<DataMode,Repetition> modeMap;
  private static Map<MinorType,OriginalType> originalTypeMap;

  static {
    typeMap = new HashMap();

    <#list vv.types as type>
    <#list type.minor as minor>
    <#if    minor.class == "TinyInt" ||
            minor.class == "UInt1" ||
            minor.class == "UInt2" ||
            minor.class == "SmallInt" ||
            minor.class == "Int" ||
            minor.class == "Time" ||
            minor.class == "Decimal9" ||
            minor.class == "Date" ||
            minor.class == "UInt4">
                    typeMap.put(MinorType.${minor.class?upper_case}, PrimitiveTypeName.INT32);
    <#elseif
            minor.class == "Float4">
                    typeMap.put(MinorType.${minor.class?upper_case}, PrimitiveTypeName.FLOAT);
    <#elseif
            minor.class == "BigInt" ||
            minor.class == "Decimal18" ||
            minor.class == "TimeStamp" ||
            minor.class == "UInt8">
                    typeMap.put(MinorType.${minor.class?upper_case}, PrimitiveTypeName.INT64);
    <#elseif
            minor.class == "Float8">
                    typeMap.put(MinorType.${minor.class?upper_case}, PrimitiveTypeName.DOUBLE);
    <#elseif
            minor.class == "Bit">
                    typeMap.put(MinorType.${minor.class?upper_case}, PrimitiveTypeName.BOOLEAN);
    <#elseif
            minor.class == "TimeTZ" ||
            minor.class == "IntervalDay" ||
            minor.class == "IntervalYear" ||
            minor.class == "Interval" ||
            minor.class == "Decimal28Dense" ||
            minor.class == "Decimal38Dense" ||
            minor.class == "Decimal28Sparse" ||
            minor.class == "Decimal38Sparse" ||
            minor.class == "VarDecimal">
                    typeMap.put(MinorType.${minor.class?upper_case}, PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY);
    <#elseif
            minor.class == "VarChar" ||
            minor.class == "Var16Char" ||
            minor.class == "VarBinary" >
                    typeMap.put(MinorType.${minor.class?upper_case}, PrimitiveTypeName.BINARY);
    </#if>
    </#list>
    </#list>

    modeMap = new HashMap();

    modeMap.put(DataMode.REQUIRED, Repetition.REQUIRED);
    modeMap.put(DataMode.OPTIONAL, Repetition.OPTIONAL);
    modeMap.put(DataMode.REPEATED, Repetition.REPEATED);

    originalTypeMap = new HashMap();

    <#list vv.types as type>
    <#list type.minor as minor>
            <#if minor.class.contains("Decimal")>
            originalTypeMap.put(MinorType.${minor.class?upper_case}, OriginalType.DECIMAL);
            </#if>
    </#list>
    </#list>
            originalTypeMap.put(MinorType.VARCHAR, OriginalType.UTF8);
            originalTypeMap.put(MinorType.DATE, OriginalType.DATE);
            originalTypeMap.put(MinorType.TIME, OriginalType.TIME_MILLIS);
            originalTypeMap.put(MinorType.TIMESTAMP, OriginalType.TIMESTAMP_MILLIS);
            originalTypeMap.put(MinorType.INTERVALDAY, OriginalType.INTERVAL);
            originalTypeMap.put(MinorType.INTERVALYEAR, OriginalType.INTERVAL);
            originalTypeMap.put(MinorType.INTERVAL, OriginalType.INTERVAL);
//            originalTypeMap.put(MinorType.TIMESTAMPTZ, OriginalType.TIMESTAMPTZ);
  }

  public static PrimitiveTypeName getPrimitiveTypeNameForMinorType(MinorType minorType) {
    return typeMap.get(minorType);
  }

  public static Repetition getRepetitionForDataMode(DataMode dataMode) {
    return modeMap.get(dataMode);
  }

  public static OriginalType getOriginalTypeForMinorType(MinorType minorType) {
    return originalTypeMap.get(minorType);
  }

  public static DecimalMetadata getDecimalMetadataForField(MaterializedField field) {
    switch(field.getType().getMinorType()) {
      case DECIMAL9:
      case DECIMAL18:
      case DECIMAL28SPARSE:
      case DECIMAL28DENSE:
      case DECIMAL38SPARSE:
      case DECIMAL38DENSE:
      case VARDECIMAL:
        return new DecimalMetadata(field.getPrecision(), field.getScale());
      default:
        return null;
    }
  }

  public static int getLengthForMinorType(MinorType minorType) {
    switch(minorType) {
      case INTERVALDAY:
      case INTERVALYEAR:
      case INTERVAL:
        return 12;
      case DECIMAL28SPARSE:
        return 12;
      case DECIMAL38SPARSE:
      case VARDECIMAL:
        return 16;
      default:
        return 0;
    }
  }

  public static int getMaxPrecisionForPrimitiveType(PrimitiveTypeName type) {
    switch(type) {
      case INT32:
        return 9;
      case INT64:
        return 18;
      case FIXED_LEN_BYTE_ARRAY:
        return DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM.getMaxNumericPrecision();
      default:
        throw new UnsupportedOperationException(String.format(
          "Specified PrimitiveTypeName %s cannot be used to determine max precision",
          type));
    }
  }

}

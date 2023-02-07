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
package org.apache.drill.exec.store.parquet.columnreaders;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MinorType;

import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.parquet.ParquetReaderUtility;
import org.apache.parquet.format.ConvertedType;
import org.apache.parquet.format.SchemaElement;
import org.apache.parquet.schema.PrimitiveType;

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkArgument;

public class ParquetToDrillTypeConverter {

  private static TypeProtos.MinorType getMinorType(PrimitiveType.PrimitiveTypeName primitiveTypeName, int length,
      ConvertedType convertedType, OptionManager options) {
    switch (primitiveTypeName) {
      case BINARY:
        if (convertedType == null) {
          return (TypeProtos.MinorType.VARBINARY);
        }
        switch (convertedType) {
          case UTF8:
          case ENUM:
            return (TypeProtos.MinorType.VARCHAR);
          case DECIMAL:
            ParquetReaderUtility.checkDecimalTypeEnabled(options);
            return TypeProtos.MinorType.VARDECIMAL;
          default:
            return (TypeProtos.MinorType.VARBINARY);
        }
      case INT64:
        if (convertedType == null) {
          return (TypeProtos.MinorType.BIGINT);
        }
        switch(convertedType) {
          case INT_64:
            return TypeProtos.MinorType.BIGINT;
          case UINT_64:
            return TypeProtos.MinorType.UINT8;
          case DECIMAL:
            ParquetReaderUtility.checkDecimalTypeEnabled(options);
            return TypeProtos.MinorType.VARDECIMAL;
          case TIMESTAMP_MILLIS:
          case TIMESTAMP_MICROS:
            return TypeProtos.MinorType.TIMESTAMP;
          case TIME_MICROS:
            return TypeProtos.MinorType.TIME;
          default:
            throw new UnsupportedOperationException(String.format("unsupported type: %s %s", primitiveTypeName, convertedType));
        }
      case INT32:
        if (convertedType == null) {
          return TypeProtos.MinorType.INT;
        }
        switch(convertedType) {
          case UINT_8:
          case UINT_16:
          case UINT_32:
            return TypeProtos.MinorType.UINT4;
          case INT_8:
          case INT_16:
          case INT_32:
            return TypeProtos.MinorType.INT;
          case DECIMAL:
            ParquetReaderUtility.checkDecimalTypeEnabled(options);
            return TypeProtos.MinorType.VARDECIMAL;
          case DATE:
            return TypeProtos.MinorType.DATE;
          case TIME_MILLIS:
            return TypeProtos.MinorType.TIME;
          default:
            throw new UnsupportedOperationException(String.format("unsupported type: %s %s", primitiveTypeName, convertedType));
        }
      case BOOLEAN:
        return TypeProtos.MinorType.BIT;
      case FLOAT:
        return TypeProtos.MinorType.FLOAT4;
      case DOUBLE:
        return TypeProtos.MinorType.FLOAT8;
      // TODO - Both of these are not supported by the parquet library yet (7/3/13),
      // but they are declared here for when they are implemented
      case INT96:
        if (options.getOption(ExecConstants.PARQUET_READER_INT96_AS_TIMESTAMP).bool_val) {
          return TypeProtos.MinorType.TIMESTAMP;
        } else {
          return TypeProtos.MinorType.VARBINARY;
        }
      case FIXED_LEN_BYTE_ARRAY:
        if (convertedType == null) {
          checkArgument(length > 0, "A length greater than zero must be provided for a FixedBinary type.");
          return TypeProtos.MinorType.VARBINARY;
        } else if (convertedType == ConvertedType.DECIMAL) {
          ParquetReaderUtility.checkDecimalTypeEnabled(options);
          return TypeProtos.MinorType.VARDECIMAL;
        } else if (convertedType == ConvertedType.INTERVAL) {
          return TypeProtos.MinorType.INTERVAL;
        }
      default:
        throw new UnsupportedOperationException("Type not supported: " + primitiveTypeName);
    }
  }

  public static TypeProtos.MajorType toMajorType(PrimitiveType.PrimitiveTypeName primitiveTypeName, int length,
                                          TypeProtos.DataMode mode, SchemaElement schemaElement,
                                          OptionManager options) {
    ConvertedType convertedType = schemaElement.getConverted_type();
    MinorType minorType = getMinorType(primitiveTypeName, length, convertedType, options);
    TypeProtos.MajorType.Builder typeBuilder = TypeProtos.MajorType.newBuilder().setMinorType(minorType).setMode(mode);

    if (Types.isDecimalType(minorType)) {
      int precision = schemaElement.getPrecision();
      int scale = schemaElement.getScale();

      typeBuilder.setPrecision(precision).setScale(scale);
    }
    return typeBuilder.build();
  }

}

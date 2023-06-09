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
package org.apache.drill.common.types;

import static org.apache.drill.common.types.TypeProtos.DataMode.REPEATED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;

import com.google.protobuf.TextFormat;

public class Types {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Types.class);

  public static final int MAX_VARCHAR_LENGTH = 65535;
  public static final int UNDEFINED = 0;

  public static final MajorType NULL = required(MinorType.NULL);
  public static final MajorType LATE_BIND_TYPE = optional(MinorType.LATE);
  public static final MajorType REQUIRED_BIT = required(MinorType.BIT);
  public static final MajorType OPTIONAL_BIT = optional(MinorType.BIT);
  public static final MajorType OPTIONAL_INT = optional(MinorType.INT);

  public static boolean isUnion(MajorType toType) {
    return toType.getMinorType() == MinorType.UNION;
  }

  public static boolean isComplex(final MajorType type) {
    switch(type.getMinorType()) {
    case LIST:
    case MAP:
    case DICT:
      return true;
    default:
      return false;
    }
  }

  public static boolean isRepeated(final MajorType type) {
    return type.getMode() == REPEATED;
  }

  public static boolean isNumericType(final MajorType type) {
    if (type.getMode() == REPEATED) {
      return false;
    }
    return isNumericType(type.getMinorType());
  }

  public static boolean isNumericType(final MinorType type) {
    switch (type) {
    case BIGINT:
    case VARDECIMAL:
    case DECIMAL38SPARSE:
    case DECIMAL38DENSE:
    case DECIMAL28SPARSE:
    case DECIMAL28DENSE:
    case DECIMAL18:
    case DECIMAL9:
    case FLOAT4:
    case FLOAT8:
    case INT:
    case MONEY:
    case SMALLINT:
    case TINYINT:
    case UINT1:
    case UINT2:
    case UINT4:
    case UINT8:
      return true;
    default:
      return false;
    }
  }

  public static boolean isDateTimeType(MajorType type) {
    if (type.getMode() == REPEATED) {
      return false;
    }
    return isDateTimeType(type.getMinorType());
  }

  public static boolean isDateTimeType(MinorType type) {
    switch (type) {
      case TIME:
      case TIMETZ:
      case DATE:
      case TIMESTAMP:
      case TIMESTAMPTZ:
        return true;
      default:
        return false;
    }
  }

  public static boolean isIntervalType(MajorType type) {
    if (type.getMode() == REPEATED) {
      return false;
    }
    return isIntervalType(type.getMinorType());
  }

  public static boolean isIntervalType(MinorType type) {
    switch (type) {
      case INTERVAL:
      case INTERVALDAY:
      case INTERVALYEAR:
        return true;
      default:
        return false;
    }
  }

  /**
   * Returns true if all specified types are decimal data types.
   *
   * @param types types to check
   * @return true if all specified types are decimal data type.
   */
  public static boolean areDecimalTypes(MinorType... types) {
    return Arrays.stream(types)
        .allMatch(Types::isDecimalType);
  }

  /**
   * Returns true if specified type is decimal data type.
   *
   * @param type type to check
   * @return true if specified type is decimal data type.
   */
  public static boolean isDecimalType(MajorType type) {
    return isDecimalType(type.getMinorType());
  }

  /**
   * Returns true if specified type is decimal data type.
   *
   * @param minorType type to check
   * @return true if specified type is decimal data type.
   */
  public static boolean isDecimalType(MinorType minorType) {
    switch(minorType) {
      case VARDECIMAL:
      case DECIMAL38SPARSE:
      case DECIMAL38DENSE:
      case DECIMAL28SPARSE:
      case DECIMAL28DENSE:
      case DECIMAL18:
      case DECIMAL9:
        return true;
      default:
        return false;
    }
  }

  /***
   * Gets SQL data type name for given Drill RPC-/protobuf-level data type.
   * @return
   *   canonical keyword sequence for SQL data type (leading keywords in
   *   corresponding {@code <data type>}; what
   *   {@code INFORMATION_SCHEMA.COLUMNS.TYPE_NAME} would list)
   */
  public static String getSqlTypeName(final MajorType type) {
    if (type.getMode() == DataMode.REPEATED || type.getMinorType() == MinorType.LIST) {
      return "ARRAY";
    }
    return getBaseSqlTypeName(type);
  }

  public static String getBaseSqlTypeName(final MajorType type) {

    switch (type.getMinorType()) {

      // Standard SQL atomic data types:

      case BIT:             return "BOOLEAN";

      case SMALLINT:        return "SMALLINT";
      case INT:             return "INTEGER";
      case BIGINT:          return "BIGINT";

      case FLOAT4:          return "FLOAT";
      case FLOAT8:          return "DOUBLE";

      case VARDECIMAL:
      case DECIMAL9:
      case DECIMAL18:
      case DECIMAL28DENSE:
      case DECIMAL28SPARSE:
      case DECIMAL38DENSE:
      case DECIMAL38SPARSE: return "DECIMAL";

      case VARCHAR:         return "CHARACTER VARYING";
      case FIXEDCHAR:       return "CHARACTER";

      case VAR16CHAR:       return "NATIONAL CHARACTER VARYING";
      case FIXED16CHAR:     return "NATIONAL CHARACTER";

      case VARBINARY:       return "BINARY VARYING";
      case FIXEDBINARY:     return "BINARY";

      case DATE:            return "DATE";
      case TIME:            return "TIME";
      case TIMETZ:          return "TIME WITH TIME ZONE";
      case TIMESTAMP:       return "TIMESTAMP";
      case TIMESTAMPTZ:     return "TIMESTAMP WITH TIME ZONE";

      case INTERVALYEAR:    return "INTERVAL YEAR TO MONTH";
      case INTERVALDAY:     return "INTERVAL DAY TO SECOND";

      // Non-standard SQL atomic data types:

      case INTERVAL:        return "INTERVAL";
      case MONEY:           return "DECIMAL";
      case TINYINT:         return "TINYINT";

      // Composite types and other types that are not atomic types (SQL standard
      // or not) except ARRAY types (handled above):

      case MAP:             return "STRUCT"; // Drill map represents struct
      case DICT:            return "MAP";
      case LATE:            return "ANY";
      case NULL:            return "NULL";
      case UNION:           return "UNION";
      case GENERIC_OBJECT:  return "JAVA_OBJECT";
      case LIST:            return "LIST";

      // Internal types not actually used at level of SQL types(?):

      case UINT1:          return "TINYINT";
      case UINT2:          return "SMALLINT";
      case UINT4:          return "INTEGER";
      case UINT8:          return "BIGINT";

      default:
        throw new AssertionError(
            "Unexpected/unhandled MinorType value " + type.getMinorType() );
    }
  }

  /**
   * Extend decimal type with precision and scale.
   *
   * @param type major type
   * @return type name augmented with precision and scale,
   * if type is a decimal
   */
  public static String getExtendedSqlTypeName(MajorType type) {
    String typeName = getBaseSqlTypeName(type);
    switch (type.getMinorType()) {
    case LIST:
      typeName = "ARRAY";
      break;
    case DECIMAL9:
    case DECIMAL18:
    case DECIMAL28SPARSE:
    case DECIMAL28DENSE:
    case DECIMAL38SPARSE:
    case DECIMAL38DENSE:
    case VARDECIMAL:
      // Disabled for now. See DRILL-6378
      if (type.getPrecision() > 0) {
        typeName += String.format("(%d, %d)",
            type.getPrecision(), type.getScale());
      }
    default:
    }
    return typeName;
  }

  public static String getSqlModeName(final MajorType type) {
    switch (type.getMode()) {
    case REQUIRED:
      return "NOT NULL";
    case OPTIONAL:
      return "NULLABLE";
    case REPEATED:
      return "ARRAY";
    default:
      return "UNKNOWN";
    }
  }

  /***
   * Gets JDBC type code for given SQL data type name.
   */
  public static int getJdbcTypeCode(final String sqlTypeName) {

    switch (sqlTypeName) {
      case "ANY":                           return java.sql.Types.OTHER;
      case "ARRAY":                         return java.sql.Types.OTHER; // Drill doesn't support java.sql.Array
      case "BIGINT":                        return java.sql.Types.BIGINT;
      case "BINARY VARYING":                return java.sql.Types.VARBINARY;
      case "BINARY":                        return java.sql.Types.BINARY;
      case "BOOLEAN":                       return java.sql.Types.BOOLEAN;
      case "CHARACTER VARYING":             return java.sql.Types.VARCHAR;
      case "CHARACTER":                     return java.sql.Types.NCHAR;
      case "DATE":                          return java.sql.Types.DATE;
      case "DECIMAL":                       return java.sql.Types.DECIMAL;
      case "DOUBLE":                        return java.sql.Types.DOUBLE;
      case "FLOAT":                         return java.sql.Types.FLOAT;
      case "INTEGER":                       return java.sql.Types.INTEGER;
      case "INTERVAL":                      return java.sql.Types.OTHER;  // JDBC (4.1) has nothing for INTERVAL
      case "INTERVAL YEAR TO MONTH":        return java.sql.Types.OTHER;
      case "INTERVAL DAY TO SECOND":        return java.sql.Types.OTHER;
      case "STRUCT":                        return java.sql.Types.OTHER; // Drill doesn't support java.sql.Struct
      case "MAP":                           return java.sql.Types.OTHER;
      case "NATIONAL CHARACTER VARYING":    return java.sql.Types.NVARCHAR;
      case "NATIONAL CHARACTER":            return java.sql.Types.NCHAR;
      case "NULL":                          return java.sql.Types.NULL;
      case "SMALLINT":                      return java.sql.Types.SMALLINT;
      case "TIME WITH TIME ZONE":           // fall through
      case "TIME":                          return java.sql.Types.TIME;
      case "TIMESTAMP WITH TIME ZONE":      // fall through
      case "TIMESTAMP":                     return java.sql.Types.TIMESTAMP;
      case "TINYINT":                       return java.sql.Types.TINYINT;
      case "UNION":                         return java.sql.Types.OTHER;
      case "JAVA_OBJECT":                   return java.sql.Types.JAVA_OBJECT;
      default:
        // TODO:  This isn't really an unsupported-operation/-type case; this
        //   is an unexpected, code-out-of-sync-with-itself case, so use an
        //   exception intended for that.
        throw new UnsupportedOperationException(
            "Unexpected/unhandled SqlType value " + sqlTypeName );
    }
  }

  /**
   * Reports whether given RPC-level type is a signed type (per semantics of
   * {@link java.sql.ResultSetMetaData#isSigned(int)}).
   */
  public static boolean isJdbcSignedType( final MajorType type ) {
    final boolean isSigned;
    switch ( type.getMode() ) {
      case REPEATED:
        isSigned = false;   // SQL ARRAY
        break;
      case REQUIRED:
      case OPTIONAL:
        switch ( type.getMinorType() ) {
          // Verified signed types:
          case SMALLINT:
          case INT:             // SQL INTEGER
          case BIGINT:
          case FLOAT4:          // SQL REAL / FLOAT(N)
          case FLOAT8:          // SQL DOUBLE PRECISION / FLOAT(N)
          case INTERVALYEAR:    // SQL INTERVAL w/YEAR and/or MONTH
          case INTERVALDAY:     // SQL INTERVAL w/DAY, HOUR, MINUTE and/or SECOND
          // Not-yet seen/verified signed types:
          case VARDECIMAL:      // SQL DECIMAL (if used)
          case DECIMAL9:        // SQL DECIMAL (if used)
          case DECIMAL18:       // SQL DECIMAL (if used)
          case DECIMAL28SPARSE: // SQL DECIMAL (if used)
          case DECIMAL38SPARSE: // SQL DECIMAL (if used)
          case DECIMAL28DENSE:  // SQL DECIMAL (if used)
          case DECIMAL38DENSE:  // SQL DECIMAL (if used)
          case TINYINT:         // (not standard SQL)
          case MONEY:           // (not standard SQL)
          case INTERVAL:        // unknown (given INTERVALYEAR and INTERVALDAY)
            isSigned = true;
            break;
          // Verified unsigned types:
          case BIT:            // SQL BOOLEAN
          case VARCHAR:
          case FIXEDCHAR:      // SQL CHARACTER
          case VARBINARY:
          case FIXEDBINARY:    // SQL BINARY
          case DATE:
          case TIME:           // SQL TIME WITHOUT TIME ZONE
          case TIMESTAMP:      // SQL TIMESTAMP WITHOUT TIME ZONE
          // Not-yet seen/verified unsigned types:
          case UINT1:
          case UINT2:
          case UINT4:
          case UINT8:
          case FIXED16CHAR:
          case VAR16CHAR:
          case GENERIC_OBJECT:
          case LATE:
          case LIST:
          case MAP:
          case DICT:
          case UNION:
          case NULL:
          case TIMETZ:      // SQL TIME WITH TIME ZONE
          case TIMESTAMPTZ: // SQL TIMESTAMP WITH TIME ZONE
            isSigned = false;
            break;
          default:
            throw new UnsupportedOperationException(
                "Unexpected/unhandled MinorType value " + type.getMinorType() );
        }
        break;
      default:
        throw new UnsupportedOperationException(
            "Unexpected/unhandled DataMode value " + type.getMode() );
    }
    return isSigned;
  }

  public static int getJdbcDisplaySize(MajorType type) {
    if (type.getMode() == DataMode.REPEATED || type.getMinorType() == MinorType.LIST) {
      return UNDEFINED;
    }

    final int precision = getPrecision(type);
    switch(type.getMinorType()) {
      case BIT:             return 1; // 1 digit

      case TINYINT:         return 4; // sign + 3 digit
      case SMALLINT:        return 6; // sign + 5 digits
      case INT:             return 11; // sign + 10 digits
      case BIGINT:          return 20; // sign + 19 digits

      case UINT1:          return 3; // 3 digits
      case UINT2:          return 5; // 5 digits
      case UINT4:          return 10; // 10 digits
      case UINT8:          return 19; // 19 digits

      case FLOAT4:          return 14; // sign + 7 digits + decimal point + E + 2 digits
      case FLOAT8:          return 24; // sign + 15 digits + decimal point + E + 3 digits

      case DECIMAL9:
      case DECIMAL18:
      case DECIMAL28DENSE:
      case DECIMAL28SPARSE:
      case DECIMAL38DENSE:
      case DECIMAL38SPARSE:
      case VARDECIMAL:
      case MONEY:           return 2 + precision; // precision of the column plus a sign and a decimal point

      case VARCHAR:
      case FIXEDCHAR:
      case VAR16CHAR:
      case FIXED16CHAR:     return precision; // number of characters

      case VARBINARY:
      case FIXEDBINARY:     return 2 * precision; // each binary byte is represented as a 2digit hex number

      case DATE:            return 10; // yyyy-mm-dd
      case TIME:
        return precision > 0
            ? 9 + precision // hh-mm-ss.SSS
            : 8; // hh-mm-ss
      case TIMETZ:
        return precision > 0
            ? 15 + precision // hh-mm-ss.SSS-zz:zz
            : 14; // hh-mm-ss-zz:zz
      case TIMESTAMP:
        return precision > 0
            ? 20 + precision // yyyy-mm-ddThh:mm:ss.SSS
            : 19; // yyyy-mm-ddThh:mm:ss
      case TIMESTAMPTZ:
        return precision > 0
            ? 26 + precision // yyyy-mm-ddThh:mm:ss.SSS:ZZ-ZZ
            : 25; // yyyy-mm-ddThh:mm:ss-ZZ:ZZ

      case INTERVALYEAR:
        return precision > 0
            ? 5 + precision // P..Y12M
            : 9; // we assume max is P9999Y12M

      case INTERVALDAY:
        return precision > 0
            ? 12 + precision // P..DT12H60M60S assuming fractional seconds precision is not supported
            : 22; // the first 4 bytes give the number of days, so we assume max is P2147483648DT12H60M60S

      case INTERVAL:
      case MAP:
      case DICT:
      case LATE:
      case NULL:
      case UNION:
        return UNDEFINED;

      default:
        throw new UnsupportedOperationException("Unexpected/unhandled MinorType value " + type.getMinorType());
    }
  }
  public static boolean usesHolderForGet(final MajorType type) {
    if (type.getMode() == REPEATED) {
      return true;
    }
    switch(type.getMinorType()) {
    case BIGINT:
    case FLOAT4:
    case FLOAT8:
    case INT:
    case MONEY:
    case SMALLINT:
    case TINYINT:
    case UINT1:
    case UINT2:
    case UINT4:
    case UINT8:
    case INTERVALYEAR:
    case DATE:
    case TIME:
    case TIMESTAMP:
      return false;

    default:
      return true;
    }
  }

  public static boolean isFixedWidthType(final MajorType type) {
    return isFixedWidthType(type.getMinorType());
  }

  public static boolean isFixedWidthType(final MinorType type) {
    return ! isVarWidthType(type);
  }

  public static boolean isVarWidthType(final MinorType type) {
    switch (type) {
    case VARBINARY:
    case VAR16CHAR:
    case VARCHAR:
    case UNION:
    case VARDECIMAL:
      return true;
    default:
      return false;
    }
  }

  /**
   * Checks if given major type is string scalar type.
   *
   * @param type major type
   * @return true if given major type is scalar string, false otherwise
   */
  public static boolean isScalarStringType(final MajorType type) {
    if (type.getMode() == REPEATED) {
      return false;
    }
    switch (type.getMinorType()) {
    case FIXEDCHAR:
    case FIXED16CHAR:
    case VARCHAR:
    case VAR16CHAR:
      return true;
    default:
      return false;
    }
  }

  public static boolean softEquals(final MajorType a, final MajorType b, final boolean allowNullSwap) {
    if (a.getMinorType() != b.getMinorType()) {
      return false;
    }
    if (allowNullSwap) {
      switch (a.getMode()) {
      case OPTIONAL:
      case REQUIRED:
        switch (b.getMode()) {
        case OPTIONAL:
        case REQUIRED:
          return true;
        default:
        }
      default:
      }
    }
    return a.getMode() == b.getMode();
  }

  public static boolean isUntypedNull(final MajorType type) {
    return type.getMinorType() == MinorType.NULL;
  }

  public static MajorType withMode(final MinorType type, final DataMode mode) {
    return MajorType.newBuilder().setMode(mode).setMinorType(type).build();
  }

  /**
   * Builds major type using given minor type, data mode and precision.
   *
   * @param type minor type
   * @param mode data mode
   * @param precision precision value
   * @return major type
   */
  public static MajorType withPrecision(final MinorType type, final DataMode mode, final int precision) {
    return MajorType.newBuilder().setMinorType(type).setMode(mode).setPrecision(precision).build();
  }

  public static MajorType withPrecisionAndScale(MinorType type, DataMode mode, int precision, int scale) {
    return MajorType.newBuilder().setMinorType(type).setMode(mode).setScale(scale).setPrecision(precision).build();
  }

  public static MajorType required(final MinorType type) {
    return MajorType.newBuilder().setMode(DataMode.REQUIRED).setMinorType(type).build();
  }

  public static MajorType repeated(final MinorType type) {
    return MajorType.newBuilder().setMode(REPEATED).setMinorType(type).build();
  }

  public static MajorType optional(final MinorType type) {
    return MajorType.newBuilder().setMode(DataMode.OPTIONAL).setMinorType(type).build();
  }

  public static MajorType overrideMode(final MajorType originalMajorType, final DataMode overrideMode) {
    return originalMajorType.toBuilder().setMode(overrideMode).build();
  }

  public static MajorType getMajorTypeFromName(final String typeName) {
    return getMajorTypeFromName(typeName, DataMode.REQUIRED);
  }

  public static MinorType getMinorTypeFromName(String typeName) {
    typeName = typeName.toLowerCase();
    switch (typeName) {
    case "bool":
    case "boolean":
      return MinorType.BIT;
    case "tinyint":
      return MinorType.TINYINT;
    case "uint1":
      return MinorType.UINT1;
    case "smallint":
      return MinorType.SMALLINT;
    case "uint2":
      return MinorType.UINT2;
    case "integer":
    case "int":
      return MinorType.INT;
    case "uint4":
      return MinorType.UINT4;
    case "bigint":
      return MinorType.BIGINT;
    case "uint8":
      return MinorType.UINT8;
    case "float":
      return MinorType.FLOAT4;
    case "double":
      return MinorType.FLOAT8;
    case "decimal":
      return MinorType.VARDECIMAL;
    case "symbol":
    case "char":
    case "utf8":
    case "varchar":
      return MinorType.VARCHAR;
    case "utf16":
    case "string":
    case "var16char":
      return MinorType.VAR16CHAR;
    case "timestamp":
      return MinorType.TIMESTAMP;
    case "interval_year_month":
      return MinorType.INTERVALYEAR;
    case "interval_day_time":
      return MinorType.INTERVALDAY;
    case "date":
      return MinorType.DATE;
    case "time":
      return MinorType.TIME;
    case "binary":
      return MinorType.VARBINARY;
    case "json":
    case "simplejson":
    case "extendedjson":
      return MinorType.LATE;
    case "null":
    case "any":
      return MinorType.NULL;
    default:
      throw new UnsupportedOperationException("Could not determine type: " + typeName);
    }
  }

  public static MajorType getMajorTypeFromName(final String typeName, final DataMode mode) {
    return withMode(getMinorTypeFromName(typeName), mode);
  }

  public static String getNameOfMinorType(final MinorType type) {
    switch (type) {
      case BIT:
        return "bool";
      case TINYINT:
        return "tinyint";
      case UINT1:
        return "uint1";
      case SMALLINT:
        return "smallint";
      case UINT2:
        return "uint2";
      case INT:
        return "int";
      case UINT4:
        return "uint4";
      case BIGINT:
        return "bigint";
      case UINT8:
        return "uint8";
      case FLOAT4:
        return "float";
      case FLOAT8:
        return "double";
      case VARDECIMAL:
      case DECIMAL9:
      case DECIMAL18:
      case DECIMAL28SPARSE:
      case DECIMAL38SPARSE:
        return "decimal";
      case VARCHAR:
        return "varchar";
      case VAR16CHAR:
        return "utf16";
      case DATE:
        return "date";
      case TIME:
        return "time";
      case TIMESTAMP:
        return "timestamp";
      case VARBINARY:
        return "binary";
      case LATE:
        throw new DrillRuntimeException("The late type should never appear in execution or an SQL query, so it does not have a name to refer to it.");
      default:
        throw new DrillRuntimeException("Unrecognized type " + type);
    }
  }

  public static String toString(final MajorType type) {
    return type != null ? "MajorType[" + TextFormat.shortDebugString(type) + "]" : "null";
  }

  /**
   * Get the <code>precision</code> of given type.
   *
   * @param majorType major type
   * @return precision value
   */
  public static int getPrecision(MajorType majorType) {
    if (majorType.hasPrecision()) {
      return majorType.getPrecision();
    }

    return isScalarStringType(majorType) ? MAX_VARCHAR_LENGTH : UNDEFINED;
  }

  /**
   * Get the <code>scale</code> of given type.
   *
   * @param majorType major type
   * @return scale value
   */
  public static int getScale(MajorType majorType) {
    if (majorType.hasScale()) {
      return majorType.getScale();
    }

    return UNDEFINED;
  }

  /**
   * Checks if the given type column can be used in ORDER BY clause.
   *
   * @param type minor type
   * @return true if type can be used in ORDER BY clause
   */
  public static boolean isSortable(MinorType type) {
    switch (type) {
      case DICT:
      case LIST:
      case MAP:
        return false;
      default:
        return true;
    }
  }

  /**
   * Sets max precision from both types if these types are string scalar types.
   * Sets max precision and scale from both types if these types are decimal types.
   * Both types should be of the same minor type.
   *
   * @param leftType type from left side
   * @param rightType type from right side
   * @param typeBuilder type builder
   * @return type builder
   */
  public static MajorType.Builder calculateTypePrecisionAndScale(MajorType leftType, MajorType rightType, MajorType.Builder typeBuilder) {
    if (!leftType.getMinorType().equals(rightType.getMinorType())) {
      return typeBuilder;
    }

    if (Types.isScalarStringType(leftType) && leftType.hasPrecision() && rightType.hasPrecision()) {
      return typeBuilder.setPrecision(Math.max(leftType.getPrecision(), rightType.getPrecision()));
    }

    MinorType minorType = leftType.getMinorType();
    if (isDecimalType(leftType)) {
      int scale = Math.max(leftType.getScale(), rightType.getScale());
      // resulting precision should take into account resulting scale value and be calculated as
      // sum of two components:
      // - max integer digits number (precision - scale) for left and right;
      // - resulting scale.
      // So for the case of cast(9999 as decimal(4,0)) and cast(1.23 as decimal(3,2))
      // resulting scale would be Max(0, 2) = 2 and resulting precision
      // would be Max(4 - 0, 3 - 2) + 2 = 6.
      // In this case, both values would fit into decimal(6, 2): 9999.00, 1.23
      int leftNumberOfDigits = leftType.getPrecision() - leftType.getScale();
      int rightNumberOfDigits = rightType.getPrecision() - rightType.getScale();
      int precision = Math.max(leftNumberOfDigits, rightNumberOfDigits) + scale;
      int maxPrecision = maxPrecision(minorType);

      if (precision > maxPrecision) {
        logger.warn(
          "Possible loss of precision: wanted {}({}, {}) but limited to {}({}, {})",
          minorType, precision, scale,
          minorType, maxPrecision, scale
        );
        precision = maxPrecision;
      }

      typeBuilder.setPrecision(precision);
      typeBuilder.setScale(scale);
      return typeBuilder;
    }

    return typeBuilder;
  }

  /**
   * Check if two "core" types are the same, ignoring subtypes and
   * children. Primarily for non-complex types.
   *
   * @param type1 first type
   * @param type2 second type
   * @return true if the two types have the same minor type, mode,
   * precision and scale
   */
  public static boolean isSameType(MajorType type1, MajorType type2) {
    return isSameTypeAndMode(type1, type2) &&
           type1.getScale() == type2.getScale() &&
           type1.getPrecision() == type2.getPrecision();
  }

  /**
   * Check if two "core" types have the same minor type and data mode,
   * ignoring subtypes and children. Primarily for non-complex types.
   *
   * @param first  first type to check
   * @param second second type to check
   * @return {@code true} if the two types have the same minor type and mode,
   * {@code false} otherwise
   */
  public static boolean isSameTypeAndMode(MajorType first, MajorType second) {
    return first.getMinorType() == second.getMinorType()
        && first.getMode() == second.getMode();
  }

  /**
   * Requires full type equality, including fields such as precision and scale.
   * But, unset fields are equivalent to 0. Can't use the protobuf-provided
   * isEquals() which treats set and unset fields as different.
   */
  public static boolean isEquivalent(MajorType type1, MajorType type2) {

    if (!isSameType(type1, type2)) {
      return false;
    }

    // Subtypes are only for unions and are seldom used.
    if (type1.getMinorType() != MinorType.UNION) {
      return true;
    }

    final List<MinorType> subtypes1 = type1.getSubTypeList();
    final List<MinorType> subtypes2 = type2.getSubTypeList();
    if (subtypes1 == subtypes2) { // Only occurs if both are null
      return true;
    }
    if (subtypes1 == null || subtypes2 == null) {
      return false;
    }
    if (subtypes1.size() != subtypes2.size()) {
      return false;
    }

    // Now it gets slow because subtype lists are not ordered.
    final List<MinorType> copy1 = new ArrayList<>(subtypes1);
    final List<MinorType> copy2 = new ArrayList<>(subtypes2);
    Collections.sort(copy1);
    Collections.sort(copy2);
    return copy1.equals(copy2);
  }

  /**
   * The union vector is a map of types. The following method provides
   * the standard name to use in the type map. It replaces the many
   * ad-hoc appearances of this code in each reference to the map.
   *
   * @param type Drill data type
   * @return string key to use for this type in a union vector type
   * map
   */
  public static String typeKey(MinorType type) {
    return type.name().toLowerCase();
  }

  public static int maxPrecision(MinorType type) {
    switch (type) {
    case DECIMAL18:
      return 18;
    case DECIMAL28DENSE:
    case DECIMAL28SPARSE:
      return 28;
    case DECIMAL38DENSE:
    case DECIMAL38SPARSE:
      return 38;
    case DECIMAL9:
      return 9;
    case VARDECIMAL:
      return 38;
    default:
      return 0;
    }
  }

  public static boolean isNullable(final MajorType type) {
    switch (type.getMode()) {
      case REQUIRED:
      case REPEATED:
        return false;
      case OPTIONAL:
        return !isComplex(type);
      default:
        throw new UnsupportedOperationException("Unexpected/unhandled DataMode value " + type.getMode());
    }
  }
}

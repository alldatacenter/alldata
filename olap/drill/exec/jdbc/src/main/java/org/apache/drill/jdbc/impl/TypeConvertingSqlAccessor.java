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
package org.apache.drill.jdbc.impl;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.vector.accessor.InvalidAccessException;
import org.apache.drill.exec.vector.accessor.SqlAccessor;
import org.apache.drill.jdbc.SQLConversionOverflowException;

/**
 * SQL accessor implementing data type conversion for JDBC
 * {@link ResultSet}.get<i>Type</i>(<i>&lt;column ID></i>) column accessor methods.
 */
class TypeConvertingSqlAccessor implements SqlAccessor {
  private final SqlAccessor innerAccessor;

  public TypeConvertingSqlAccessor( SqlAccessor innerAccessor ) {
    this.innerAccessor = innerAccessor;
  }

  @Override
  public MajorType getType() {
    return innerAccessor.getType();
  }

  @Override
  public Class<?> getObjectClass() {
    return innerAccessor.getObjectClass();
  }

  @Override
  public boolean isNull( int rowOffset ) {
    return innerAccessor.isNull( rowOffset );
  }

  //////////////////////////////////////////////////////////////////////
  // PER JDBC specification:
  //
  // Column accessor (getXx(...)) methods and allowed source types, from
  // JDBC 4.2 specification's TABLE B-6 ("Use of ResultSet getter Methods to
  // Retrieve JDBC Data Types"; methods listed in same order as in spec (types
  // reordered for consistency):
  //
  // - getByte:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - ROWID;
  //
  // - getShort:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //
  // - getInt:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //
  // - getLong:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //
  // - getFloat:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //
  // - getDouble:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //
  // - getBigDecimal:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //
  // - getBoolean:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //
  // - getString:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - NCHAR, NVARCHAR, LONGNVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - DATE, TIME, TIMESTAMP;
  //   - DATALINK;
  //
  // - getNString:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - NCHAR, NVARCHAR, LONGNVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - DATE, TIME, TIMESTAMP;
  //   - DATALINK;
  //
  // - getBytes:
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //
  // - getDate:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - DATE, TIMESTAMP;
  //
  // - getTime:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - TIME, TIMESTAMP;
  //
  // - getTimestamp:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - DATE, TIME, TIMESTAMP;
  //
  // - getAsciiStream:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - CLOB, NCLOB;
  //
  // - getBinaryStream:
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //
  // - getCharacterStream:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - NCHAR, NVARCHAR, LONGNVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - CLOB, NCLOB;
  //   - SQLXML;
  //
  // - getNCharacterStream:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - NCHAR, NVARCHAR, LONGNVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - CLOB, NCLOB;
  //   - SQLXML;
  //
  // - getClob:
  //   - CLOB, NCLOB;
  //
  // - getNClob:
  //   - CLOB, NCLOB;
  //
  // - getBlob:
  //   - BLOB;
  //
  // - getArray:
  //   - ARRAY;
  //
  // - getRef:
  //   - REF;
  //
  // - getURL:
  //   - DATALINK;
  //
  // - getObject:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - NCHAR, NVARCHAR, LONGNVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - CLOB, NCLOB;
  //   - BLOB;
  //   - DATE, TIME, TIMESTAMP;
  //   - TIME_WITH_TIMEZONE;
  //   - TIMESTAMP_WITH_TIMEZONE;
  //   - DATALINK;
  //   - ROWID;
  //   - SQLXML;
  //   - ARRAY;
  //   - REF;
  //   - STRUCT;
  //   - JAVA_OBJECT;
  //
  // - getRowId:
  //   - ROWID;
  //
  // - getSQLXML:
  //   - SQLXML SQLXML


  /**
   * Creates SQLConversionOverflowException using given parameters to form
   * exception message text.
   *
   * @param  methodLabel  label for (string to use to identify) method that
   *                      couldn't convert source value to target type
   *                      (e.g., "getInt(...)")
   * @param  valueTypeLabel  label for type of source value that couldn't be
   *                         converted (e.g., "Java long / SQL BIGINT")
   * @param  value  representation of source value that couldn't be converted;
   *                object whose toString() shows (part of) value; not null
   * @return the created exception
   */
  private static SQLConversionOverflowException newOverflowException(
      String methodLabel, String valueTypeLabel, Object value ) {
    return new SQLConversionOverflowException(
        methodLabel + " can't return " + valueTypeLabel + " value " + value
        + " (too large) ");
  }


  //////////////////////////////////////////////////////////////////////
  // Column accessor (getXxx(...)) methods, in same order as in JDBC 4.2 spec.
  // TABLE B-6:

  ////////////////////////////////////////
  // - getByte:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT - supported/implemented (here)
  //   - REAL, FLOAT, DOUBLE - supported/implemented (here)
  //   - DECIMAL, NUMERIC- supported/implemented (here)
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - ROWID;

  /**
   * Returns byte-sized integer value or throws SQLConversionOverflowException
   * if given value doesn't fit in byte.
   */
  private static byte getByteValueOrThrow( long value, String typeLabel )
      throws SQLConversionOverflowException {
    if ( Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE ) {
      return (byte) value;
    } else {
      throw newOverflowException( "getByte(...)", typeLabel, value );
    }
  }

  /**
   * Returns byte-sized integer version of floating-point value or throws
   * SQLConversionOverflowException if given value doesn't fit in byte.
   */
  private static byte getByteValueOrThrow( double value, String typeLabel )
      throws SQLConversionOverflowException {
    if ( Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE ) {
      return (byte) value;
    } else {
      throw newOverflowException( "getByte(...)", typeLabel, value );
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   *   This implementation implements type conversions for
   *   {@link ResultSet#getByte(String)}/{@link ResultSet#getByte(int)}.
   * </p>
   */
  @Override
  public byte getByte( int rowOffset ) throws InvalidAccessException {
    final byte result;
    switch ( getType().getMinorType() ) {
      // 1. Regular type:
      case TINYINT:
        result = innerAccessor.getByte( rowOffset );
        break;

      // 2. Converted-from types:
      case SMALLINT:
        result = getByteValueOrThrow( innerAccessor.getShort( rowOffset ),
                                      "Java short / SQL SMALLINT" );
        break;
      case INT:
        result = getByteValueOrThrow( innerAccessor.getInt( rowOffset ),
                                      "Java int / SQL INTEGER" );
        break;
      case BIGINT:
        result = getByteValueOrThrow( innerAccessor.getLong( rowOffset ),
                                      "Java long / SQL BIGINT" );
        break;
      case FLOAT4:
        result = getByteValueOrThrow( innerAccessor.getFloat( rowOffset ),
                                      "Java float / SQL REAL/FLOAT" );
        break;
      case FLOAT8:
        result = getByteValueOrThrow( innerAccessor.getDouble( rowOffset ),
                                      "Java double / SQL DOUBLE PRECISION" );
        break;
      case VARDECIMAL:
        result = getByteValueOrThrow(
            innerAccessor.getBigDecimal(rowOffset).byteValue(),
            "Java BigDecimal / SQL DECIMAL PRECISION");
        break;

      // 3. Not-yet-converted and unconvertible types:
      default:
        result = innerAccessor.getByte( rowOffset );
        break;
    }
    return result;
  }

  ////////////////////////////////////////
  // - getShort:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT - supported/implemented (here)
  //   - REAL, FLOAT, DOUBLE - supported/implemented (here)
  //   - DECIMAL, NUMERIC- supported/implemented (here)
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;

  private static short getShortValueOrThrow( long value, String typeLabel )
      throws SQLConversionOverflowException {
    if ( Short.MIN_VALUE <= value && value <= Short.MAX_VALUE ) {
      return (short) value;
    } else {
      throw newOverflowException( "getShort(...)", typeLabel, value );
    }
  }

  private static short getShortValueOrThrow( double value, String typeLabel )
      throws SQLConversionOverflowException {
    if ( Short.MIN_VALUE <= value && value <= Short.MAX_VALUE ) {
      return (short) value;
    } else {
      throw newOverflowException( "getShort(...)", typeLabel, value );
    }
  }

  @Override
  public short getShort( int rowOffset ) throws InvalidAccessException {
    final short result;
    switch ( getType().getMinorType() ) {
      // 1. Regular type:
      case SMALLINT:
        result = innerAccessor.getShort( rowOffset );
        break;

      // 2. Converted-from types:
      case TINYINT:
        result = innerAccessor.getByte( rowOffset );
        break;
      case INT:
        result = getShortValueOrThrow( innerAccessor.getInt( rowOffset ),
                                       "Java int / SQL INTEGER" );
        break;
      case BIGINT:
        result = getShortValueOrThrow( innerAccessor.getLong( rowOffset ),
                                       "Java long / SQL BIGINT" );
        break;
      case FLOAT4:
        result = getShortValueOrThrow( innerAccessor.getFloat( rowOffset ),
                                       "Java float / SQL REAL/FLOAT" );
        break;
      case FLOAT8:
        result = getShortValueOrThrow( innerAccessor.getDouble( rowOffset ),
                                       "Java double / SQL DOUBLE PRECISION" );
        break;
      case VARDECIMAL:
        result = getShortValueOrThrow(
            innerAccessor.getBigDecimal(rowOffset).shortValue(),
            "Java BigDecimal / SQL DECIMAL PRECISION");
        break;

      // 3. Not-yet-converted and unconvertible types:
      default:
        result = innerAccessor.getByte( rowOffset );
        break;
    }
    return result;
  }

  ////////////////////////////////////////
  // - getInt:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT - supported/implemented (here)
  //   - REAL, FLOAT, DOUBLE - supported/implemented (here)
  //   - DECIMAL, NUMERIC- supported/implemented (here)
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;

  private static int getIntValueOrThrow( long value, String typeLabel )
      throws SQLConversionOverflowException {
    if ( Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE ) {
      return (int) value;
    } else {
      throw newOverflowException( "getInt(...)", typeLabel, value );
    }
  }

  private static int getIntValueOrThrow( double value, String typeLabel )
      throws SQLConversionOverflowException {
    if ( Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE ) {
      return (int) value;
    } else {
      throw newOverflowException( "getInt(...)", typeLabel, value );
    }
  }

  @Override
  public int getInt( int rowOffset ) throws InvalidAccessException {
    final int result;
    switch ( getType().getMinorType() ) {
      // 1. Regular type:
      case INT:
        result = innerAccessor.getInt( rowOffset );
        break;

      // 2. Converted-from types:
      case TINYINT:
        result = innerAccessor.getByte( rowOffset );
        break;
      case SMALLINT:
        result = innerAccessor.getShort( rowOffset );
        break;
      case BIGINT:
        result = getIntValueOrThrow( innerAccessor.getLong( rowOffset ),
                                     "Java long / SQL BIGINT" );
        break;
      case FLOAT4:
        result = getIntValueOrThrow( innerAccessor.getFloat( rowOffset ),
                                     "Java float / SQL REAL/FLOAT" );
        break;
      case FLOAT8:
        result = getIntValueOrThrow( innerAccessor.getDouble( rowOffset ),
                                     "Java double / SQL DOUBLE PRECISION" );
        break;
      case VARDECIMAL:
        result = getIntValueOrThrow(
            innerAccessor.getBigDecimal(rowOffset).intValue(),
            "Java BigDecimal / SQL DECIMAL PRECISION");
        break;

      // 3. Not-yet-converted and unconvertible types:
      default:
        result = innerAccessor.getInt( rowOffset );
        break;
    }
    return result;
  }

  ////////////////////////////////////////
  // - getLong:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT - supported/implemented (here)
  //   - REAL, FLOAT, DOUBLE - supported/implemented (here)
  //   - DECIMAL, NUMERIC- supported/implemented (here)
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;

  private static long getLongValueOrThrow( double value, String typeLabel )
      throws SQLConversionOverflowException {
    if ( Long.MIN_VALUE <= value && value <= Long.MAX_VALUE ) {
      return (long) value;
    } else {
      throw newOverflowException( "getLong(...)", typeLabel, value );
    }
  }

  @Override
  public long getLong( int rowOffset ) throws InvalidAccessException {
    final long result;
    switch ( getType().getMinorType() ) {
      // 1. Regular type:
      case BIGINT:
        result = innerAccessor.getLong( rowOffset );
        break;

      // 2. Converted-from types:
      case TINYINT:
        result = innerAccessor.getByte( rowOffset );
        break;
      case SMALLINT:
        result = innerAccessor.getShort( rowOffset );
        break;
      case INT:
        result = innerAccessor.getInt( rowOffset );
        break;
      case FLOAT4:
        result = getLongValueOrThrow( innerAccessor.getFloat( rowOffset ),
                                      "Java float / SQL REAL/FLOAT" );
        break;
      case FLOAT8:
        result = getLongValueOrThrow( innerAccessor.getDouble( rowOffset ),
                                      "Java double / SQL DOUBLE PRECISION" );
        break;
      case VARDECIMAL:
        result = getLongValueOrThrow(
            innerAccessor.getBigDecimal(rowOffset).longValue(),
            "Java BigDecimal / SQL DECIMAL PRECISION");
        break;

      // 3. Not-yet-converted and unconvertible types:
      default:
        result = innerAccessor.getLong( rowOffset );
        break;
    }
    return result;
  }

  private static float getFloatValueOrThrow(double value, String typeLabel)
      throws SQLConversionOverflowException {
    if (Float.MIN_VALUE <= value && value <= Float.MAX_VALUE) {
      return (float) value;
    } else {
      throw newOverflowException("getFloat(...)", typeLabel, value);
    }
  }

  ////////////////////////////////////////
  // - getFloat:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT - supported/implemented (here)
  //   - REAL, FLOAT, DOUBLE - supported/implemented (here)
  //   - DECIMAL, NUMERIC- supported/implemented (here)
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;

  @Override
  public float getFloat( int rowOffset ) throws InvalidAccessException {
    final float result;
    switch ( getType().getMinorType() ) {
      // 1. Regular type:
      case FLOAT4:
        result = innerAccessor.getFloat( rowOffset );
        break;

      // 2. Converted-from types:
      case INT:
        result = innerAccessor.getInt( rowOffset );
        break;
      case BIGINT:
        result = innerAccessor.getLong( rowOffset );
        break;
      case FLOAT8:
        result = getFloatValueOrThrow(innerAccessor.getDouble(rowOffset),
            "Java double / SQL DOUBLE PRECISION");
        break;
      case VARDECIMAL:
        result = getFloatValueOrThrow(
            innerAccessor.getBigDecimal(rowOffset).floatValue(),
            "Java BigDecimal / SQL DECIMAL PRECISION");
        break;

      // 3. Not-yet-converted and unconvertible types:
      default:
        result = innerAccessor.getInt( rowOffset );
        break;
    }
    return result;
  }

  ////////////////////////////////////////
  // - getDouble:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT - supported/implemented (here)
  //   - REAL, FLOAT, DOUBLE - supported/implemented (here)
  //   - DECIMAL, NUMERIC- supported/implemented (here)
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;

  @Override
  public double getDouble( int rowOffset ) throws InvalidAccessException {
    final double result;
    switch ( getType().getMinorType() ) {
      // 1. Regular type:
      case FLOAT8:
        result = innerAccessor.getDouble( rowOffset );
        break;

      // 2. Converted-from types:
      case INT:
        result = innerAccessor.getInt( rowOffset );
        break;
      case BIGINT:
        result = innerAccessor.getLong( rowOffset );
        break;
      case FLOAT4:
        result = innerAccessor.getFloat( rowOffset );
        break;
      case VARDECIMAL:
        result = innerAccessor.getBigDecimal(rowOffset).doubleValue();
        break;

      // 3. Not-yet-converted and unconvertible types:
      default:
        result = innerAccessor.getLong( rowOffset );
        //break;
    }
    return result;
  }


  ////////////////////////////////////////
  // - getBigDecimal:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT - supported/implemented (here)
  //   - REAL, FLOAT, DOUBLE - supported/implemented (here)
  //   - DECIMAL, NUMERIC- supported/implemented (here)
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;

  @Override
  public BigDecimal getBigDecimal( int rowOffset ) throws InvalidAccessException {
    final BigDecimal result;
    switch ( getType().getMinorType() ) {
      // 1. Regular type:
      case DECIMAL9:
      case DECIMAL18:
      case DECIMAL28SPARSE:
      case DECIMAL38SPARSE:
      case VARDECIMAL:
        result = innerAccessor.getBigDecimal( rowOffset );
        break;

      // 2. Converted-from types:
      case TINYINT:
        result = new BigDecimal( innerAccessor.getByte( rowOffset ) );
        break;
      case SMALLINT:
        result = new BigDecimal( innerAccessor.getShort( rowOffset ) );
        break;
      case INT:
        result = new BigDecimal( innerAccessor.getInt( rowOffset ) );
        break;
      case BIGINT:
        result = new BigDecimal( innerAccessor.getLong( rowOffset ) );
        break;
      case FLOAT4:
        result = new BigDecimal( innerAccessor.getFloat( rowOffset ) );
        break;
      case FLOAT8:
        result = new BigDecimal( innerAccessor.getDouble( rowOffset ) );
        break;

      // 3. Not-yet-converted and unconvertible types:
      default:
        result = innerAccessor.getBigDecimal( rowOffset );
        //break;
    }
    return result;
  }

  ////////////////////////////////////////
  // - getBoolean:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT;
  //   - REAL, FLOAT, DOUBLE;
  //   - DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;

  @Override
  public boolean getBoolean( int rowOffset ) throws InvalidAccessException {
    return innerAccessor.getBoolean( rowOffset );
  }

  ////////////////////////////////////////
  // - getString:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT;
  //   - REAL, FLOAT, DOUBLE;
  //   - DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - NCHAR, NVARCHAR, LONGNVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - DATE, TIME, TIMESTAMP;
  //   - DATALINK;

  @Override
  public String getString( int rowOffset ) throws InvalidAccessException {
    return innerAccessor.getString( rowOffset );
  }

  ////////////////////////////////////////
  // - getNString:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT;
  //   - REAL, FLOAT, DOUBLE;
  //   - DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - NCHAR, NVARCHAR, LONGNVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - DATE, TIME, TIMESTAMP;
  //   - DATALINK;

  ////////////////////////////////////////
  // - getBytes:
  //   - BINARY, VARBINARY, LONGVARBINARY;

  @Override
  public byte[] getBytes( int rowOffset ) throws InvalidAccessException {
    return innerAccessor.getBytes( rowOffset );
  }

  ////////////////////////////////////////
  // - getDate:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - DATE, TIMESTAMP;

  @Override
  public Date getDate( int rowOffset ) throws InvalidAccessException {
    return innerAccessor.getDate( rowOffset );
  }

  ////////////////////////////////////////
  // - getTime:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - TIME, TIMESTAMP;

  @Override
  public Time getTime( int rowOffset ) throws InvalidAccessException {
    return innerAccessor.getTime( rowOffset );
  }

  ////////////////////////////////////////
  // - getTimestamp:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - DATE, TIME, TIMESTAMP;

  @Override
  public Timestamp getTimestamp( int rowOffset ) throws InvalidAccessException {
    return innerAccessor.getTimestamp( rowOffset );
  }

  ////////////////////////////////////////
  // - getAsciiStream:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - CLOB, NCLOB;

  // Not supported or not supported via SqlAccessor(?).

  ////////////////////////////////////////
  // - getBinaryStream:
  //   - BINARY, VARBINARY, LONGVARBINARY;

  // Not supported or not supported via SqlAccessor(?).

  ////////////////////////////////////////
  // - getCharacterStream:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - NCHAR, NVARCHAR, LONGNVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - CLOB, NCLOB;
  //   - SQLXML;

  // Not supported or not supported via SqlAccessor(?).

  ////////////////////////////////////////
  // - getNCharacterStream:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - NCHAR, NVARCHAR, LONGNVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - CLOB, NCLOB;
  //   - SQLXML;

  // Not supported or not supported via SqlAccessor(?).

  ////////////////////////////////////////
  // - getClob:
  //   - CLOB, NCLOB;

  // Not supported or not supported via SqlAccessor(?).

  ////////////////////////////////////////
  // - getNClob:
  //   - CLOB, NCLOB;

  // Not supported or not supported via SqlAccessor(?).

  ////////////////////////////////////////
  // - getBlob:
  //   - BLOB;

  // Not supported or not supported via SqlAccessor(?).

  ////////////////////////////////////////
  // - getArray:
  //   - ARRAY;

  // Not supported or not supported via SqlAccessor(?).

  ////////////////////////////////////////
  // - getRef:
  //   - REF;

  // Not supported or not supported via SqlAccessor(?).

  ////////////////////////////////////////
  // - getURL:
  //   - DATALINK;

  ////////////////////////////////////////
  // - getObject:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT;
  //   - REAL, FLOAT, DOUBLE;
  //   - DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - NCHAR, NVARCHAR, LONGNVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - CLOB, NCLOB;
  //   - BLOB;
  //   - DATE, TIME, TIMESTAMP;
  //   - TIME_WITH_TIMEZONE;
  //   - TIMESTAMP_WITH_TIMEZONE;
  //   - DATALINK;
  //   - ROWID;
  //   - SQLXML;
  //   - ARRAY;
  //   - REF;
  //   - STRUCT;
  //   - JAVA_OBJECT;

  @Override
  public Object getObject( int rowOffset ) throws InvalidAccessException {
    return innerAccessor.getObject( rowOffset );
  }


  ////////////////////////////////////////
  // - getRowId:
  //   - ROWID;

  // Not supported or not supported via SqlAccessor(?).

  ////////////////////////////////////////
  // - getSQLXML:
  //   - SQLXML SQLXML

  // Not supported or not supported via SqlAccessor(?).


  // SqlAccessor getXxx(...) methods not corresponding by name with ResultSet methods:

  @Override
  public char getChar( int rowOffset ) throws InvalidAccessException {
    return innerAccessor.getChar( rowOffset );
  }

  @Override
  public InputStream getStream( int rowOffset ) throws InvalidAccessException {
    return innerAccessor.getStream( rowOffset );
  }

  @Override
  public Reader getReader( int rowOffset ) throws InvalidAccessException {
    return innerAccessor.getReader( rowOffset );
  }

}

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

import org.apache.drill.common.types.Types;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.vector.accessor.AbstractSqlAccessor;
import org.apache.drill.exec.vector.accessor.InvalidAccessException;
import org.apache.drill.exec.vector.accessor.SqlAccessor;
import org.apache.drill.jdbc.SQLConversionOverflowException;
import org.apache.drill.categories.JdbcTest;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Class-level unit test for {@link TypeConvertingSqlAccessor}.
 * (Also see {@link org.apache.drill.jdbc.ResultSetGetMethodConversionsTest}.
 */
@Category(JdbcTest.class)
public class TypeConvertingSqlAccessorTest extends BaseTest {

  /**
   * Base test stub(?) for accessors underlying TypeConvertingSqlAccessor.
   * Carries type and (Object form of) one value.
   */
  private static abstract class BaseStubAccessor extends AbstractSqlAccessor
                                                 implements SqlAccessor {
    private final MajorType type;
    private final Object value;

    BaseStubAccessor( MajorType type, Object value )
    {
      this.type = type;
      this.value = value;
    }

    @Override
    public Class<?> getObjectClass() {
      throw new RuntimeException( "Unexpected use of getObjectClass(...)" );
    }

    @Override
    public MajorType getType() {
      return type;
    }

    protected Object getValue() {
      return value;
    }

    @Override
    public boolean isNull( int rowOffset ) {
      return false;
    }

    @Override
    public Object getObject( int rowOffset ) throws InvalidAccessException {
      throw new RuntimeException( "Unexpected use of getObject(...)" );
    }

  }

  // Byte?  TinyInt?  TINYINT?
  private static class TinyIntStubAccessor extends BaseStubAccessor {
    TinyIntStubAccessor( byte value ) {
      super( Types.required( MinorType.TINYINT ), value );
    }

    public byte getByte( int rowOffset ) {
      return (Byte) getValue();
    }
  } // TinyIntStubAccessor


  // Short?  SmallInt?  SMALLINT?
  private static class SmallIntStubAccessor extends BaseStubAccessor {
    SmallIntStubAccessor( short value ) {
      super( Types.required( MinorType.SMALLINT ), value );
    }

    public short getShort( int rowOffset ) {
      return (Short) getValue();
    }
  } // SmallIntStubAccessor


  // Int?  Int?  INT?
  private static class IntegerStubAccessor extends BaseStubAccessor {
    IntegerStubAccessor( int value ) {
      super( Types.required( MinorType.INT ), value );
    }

    public int getInt( int rowOffset ) {
      return (Integer) getValue();
    }
  } // IntegerStubAccessor


  // Long?  Bigint?  BIGINT?
  private static class BigIntStubAccessor extends BaseStubAccessor {
    BigIntStubAccessor( long value ) {
      super( Types.required( MinorType.BIGINT ), value );
    }

    public long getLong( int rowOffset ) {
      return (Long) getValue();
    }
  } // BigIntStubAccessor


  // Float?  Float4?  FLOAT? (REAL?)
  private static class FloatStubAccessor extends BaseStubAccessor {
    FloatStubAccessor( float value ) {
      super( Types.required( MinorType.FLOAT4 ), value );
    }

    public float getFloat( int rowOffset ) {
      return (Float) getValue();
    }
  } // FloatStubAccessor

  // Double?  Float8?  DOUBLE?
  private static class DoubleStubAccessor extends BaseStubAccessor {
    DoubleStubAccessor( double value ) {
      super( Types.required( MinorType.FLOAT8 ), value );
    }

    public double getDouble( int rowOffset ) {
      return (double) getValue();
    }
  } // DoubleStubAccessor


  //////////////////////////////////////////////////////////////////////
  // Column accessor (getXxx(...)) methods, in same order as in JDBC 4.2 spec.
  // TABLE B-6 ("Use of ResultSet getter Methods to Retrieve JDBC Data Types"):

  ////////////////////////////////////////
  // - getByte:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - ROWID;

  @Test
  public void test_getByte_on_TINYINT_getsIt() throws InvalidAccessException {
    final SqlAccessor uut1 =
        new TypeConvertingSqlAccessor( new TinyIntStubAccessor( (byte) 127 ) );
    assertThat( uut1.getByte( 0 ), equalTo( (byte) 127 ) );
    final SqlAccessor uut2 =
        new TypeConvertingSqlAccessor( new TinyIntStubAccessor( (byte) -128 ) );
    assertThat( uut2.getByte( 0 ), equalTo( (byte) -128 ) );
  }

  @Test
  public void test_getByte_on_SMALLINT_thatFits_getsIt()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new SmallIntStubAccessor( (short) 127 ) );
    assertThat( uut.getByte( 0 ), equalTo( (byte) 127 ) );
  }

  @Test( expected = SQLConversionOverflowException.class )
  public void test_getByte_on_SMALLINT_thatOverflows_rejectsIt()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new SmallIntStubAccessor( (short) 128 ) );
    try {
      uut.getByte( 0 );
    }
    catch ( Throwable e ) {
      // Expect the too-big source value in error message:
      assertThat( e.getMessage(), containsString( "128" ) );
      // Probably expect the method name:
      assertThat( e.getMessage(), containsString( "getByte" ) );
      // Expect something about source type (original SQL type and default Java
      // type, currently).
      assertThat( e.getMessage(), allOf( containsString( "short" ),
                                         containsString( "SMALLINT" ) ) );
      throw e;
    }
  }

  @Test
  public void test_getByte_on_INTEGER_thatFits_getsIt()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new IntegerStubAccessor( -128 ) );
    assertThat( uut.getByte( 0 ), equalTo( (byte) -128 ) );
  }

  @Test( expected = SQLConversionOverflowException.class )
  public void test_getByte_on_INTEGER_thatOverflows_rejectsIt()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new IntegerStubAccessor( -129 ) );
    try {
      uut.getByte( 0 );
    }
    catch ( Throwable e ) {
      assertThat( e.getMessage(), containsString( "-129" ) );
      assertThat( e.getMessage(), containsString( "getByte" ) );
      assertThat( e.getMessage(), allOf( containsString( "int" ),
                                         containsString( "INTEGER" ) ) );
      throw e;
    }
  }

  @Test
  public void test_getByte_on_BIGINT_thatFits_getsIt()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new BigIntStubAccessor( -128 ) );
    assertThat( uut.getByte( 0 ), equalTo( (byte) -128 ) );
  }

  @Test( expected = SQLConversionOverflowException.class )
  public void test_getByte_on_BIGINT_thatOverflows_rejectsIt()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new BigIntStubAccessor( 129 ) );
    try {
      uut.getByte( 0 );
    }
    catch ( Throwable e ) {
      assertThat( e.getMessage(), containsString( "129" ) );
      assertThat( e.getMessage(), containsString( "getByte" ) );
      assertThat( e.getMessage(), allOf( containsString( "long" ),
                                         containsString( "BIGINT" ) ) );
      throw e;
    }
  }

  @Test
  public void test_getByte_on_FLOAT_thatFits_getsIt()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new FloatStubAccessor( -128.0f ) );
    assertThat( uut.getByte( 0 ), equalTo( (byte) -128 ) );
  }

  @Test( expected = SQLConversionOverflowException.class )
  public void test_getByte_on_FLOAT_thatOverflows_rejectsIt()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new FloatStubAccessor( -130f ) );
    try {
      uut.getByte( 0 );
    }
    catch ( Throwable e ) {
      assertThat( e.getMessage(), containsString( "-130" ) );
      assertThat( e.getMessage(), containsString( "getByte" ) );
      assertThat( e.getMessage(), allOf( containsString( "float" ),
                                         anyOf( containsString( "REAL" ),
                                                containsString( "FLOAT" ) ) ) );
      throw e;
    }
  }

  @Test
  public void test_getByte_on_DOUBLE_thatFits_getsIt()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new DoubleStubAccessor( 127.0d ) );
    assertThat( uut.getByte( 0 ), equalTo( (byte) 127) );
  }

  @Test( expected = SQLConversionOverflowException.class )
  public void test_getByte_on_DOUBLE_thatOverflows_rejectsIt()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new DoubleStubAccessor( -130 ) );
    try {
      uut.getByte( 0 );
    }
    catch ( Throwable e ) {
      assertThat( e.getMessage(), containsString( "-130" ) );
      assertThat( e.getMessage(), containsString( "getByte" ) );
      assertThat( e.getMessage(), allOf( containsString( "double" ),
                                         anyOf( containsString( "DOUBLE PRECISION" ),
                                                containsString( "FLOAT(" ) ) ) );
      throw e;
    }
  }

  ////////////////////////////////////////
  // - getShort:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;

  @Test
  public void test_getShort_on_TINYINT_getsIt() throws InvalidAccessException {
    final SqlAccessor uut1 =
        new TypeConvertingSqlAccessor( new TinyIntStubAccessor( (byte) 127 ) );
    assertThat( uut1.getShort( 0 ), equalTo( (short) 127 ) );
    final SqlAccessor uut2 =
        new TypeConvertingSqlAccessor( new TinyIntStubAccessor( (byte) -128 ) );
    assertThat( uut2.getShort( 0 ), equalTo( (short) -128 ) );
  }

  @Test
  public void test_getShort_on_SMALLINT_getsIt() throws InvalidAccessException {
    final SqlAccessor uut1 =
        new TypeConvertingSqlAccessor( new SmallIntStubAccessor( (short) 32767 ) );
    assertThat( uut1.getShort( 0 ), equalTo( (short) 32767 ) );
    final SqlAccessor uut2 =
        new TypeConvertingSqlAccessor( new SmallIntStubAccessor( (short) -32768 ) );
    assertThat( uut2.getShort( 0 ), equalTo( (short) -32768 ) );
  }

  @Test
  public void test_getShort_on_INTEGER_thatFits_getsIt()
      throws InvalidAccessException {
    final SqlAccessor uut1 =
        new TypeConvertingSqlAccessor( new IntegerStubAccessor( 32767 ) );
    assertThat( uut1.getShort( 0 ), equalTo( (short) 32767 ) );
    final SqlAccessor uut2 =
        new TypeConvertingSqlAccessor( new IntegerStubAccessor( -32768 ) );
    assertThat( uut2.getShort( 0 ), equalTo( (short) -32768 ) );
  }

  @Test( expected = SQLConversionOverflowException.class )
  public void test_getShort_on_INTEGER_thatOverflows_throws()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new IntegerStubAccessor( -32769 ) );
    try {
      uut.getShort( 0 );
    }
    catch ( Throwable e ) {
      assertThat( e.getMessage(), containsString( "-32769" ) );
      assertThat( e.getMessage(), containsString( "getShort" ) );
      assertThat( e.getMessage(), allOf( containsString( "int" ),
                                         containsString( "INTEGER" ) ) );
      throw e;
    }
  }

  @Test
  public void test_getShort_BIGINT_thatFits_getsIt() throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new BigIntStubAccessor( -32678 ) );
    assertThat( uut.getShort( 0 ), equalTo( (short) -32678 ) );
  }

  @Test( expected = SQLConversionOverflowException.class )
  public void test_getShort_on_BIGINT_thatOverflows_throws()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new BigIntStubAccessor( 65535 ) );
    try {
      uut.getShort( 0 );
    }
    catch ( Throwable e ) {
      assertThat( e.getMessage(), containsString( "65535" ) );
      assertThat( e.getMessage(), containsString( "getShort" ) );
      assertThat( e.getMessage(), allOf( containsString( "long" ),
                                         containsString( "BIGINT" ) ) );
      throw e;
    }
  }

  @Test
  public void test_getShort_on_FLOAT_thatFits_getsIt() throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new FloatStubAccessor( -32768f ) );
    assertThat( uut.getShort( 0 ), equalTo( (short) -32768 ) );
  }

  @Test( expected = SQLConversionOverflowException.class )
  public void test_getShort_on_FLOAT_thatOverflows_rejectsIt()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new FloatStubAccessor( -32769f ) );
    try {
      uut.getShort( 0 );
    }
    catch ( Throwable e ) {
      assertThat( e.getMessage(), containsString( "-32769" ) );
      assertThat( e.getMessage(), containsString( "getShort" ) );
      assertThat( e.getMessage(), allOf( containsString( "float" ),
                                         anyOf( containsString( "REAL" ),
                                                containsString( "FLOAT" ) ) ) );
      throw e;
    }
  }

  @Test
  public void test_getShort_on_DOUBLE_thatFits_getsIt() throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new DoubleStubAccessor( 32767d ) );
    assertThat( uut.getShort( 0 ), equalTo( (short) 32767) );
  }

  @Test( expected = SQLConversionOverflowException.class )
  public void test_getShort_on_DOUBLE_thatOverflows_rejectsIt()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new DoubleStubAccessor( 32768 ) );
    try {
      uut.getShort( 0 );
    }
    catch ( Throwable e ) {
      assertThat( e.getMessage(), containsString( "32768" ) );
      assertThat( e.getMessage(), containsString( "getShort" ) );
      assertThat( e.getMessage(), allOf( containsString( "double" ),
                                         anyOf( containsString( "DOUBLE PRECISION" ),
                                                containsString( "FLOAT" ) ) ) );
      throw e;
    }
  }


  ////////////////////////////////////////
  // - getInt:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;

  @Test
  public void test_getInt_on_TINYINT_getsIt() throws InvalidAccessException {
    final SqlAccessor uut1 =
        new TypeConvertingSqlAccessor( new TinyIntStubAccessor( (byte) 127 ) );
    assertThat( uut1.getInt( 0 ), equalTo( 127 ) );
    final SqlAccessor uut2 =
        new TypeConvertingSqlAccessor( new TinyIntStubAccessor( (byte) -128 ) );
    assertThat( uut2.getInt( 0 ), equalTo( -128 ) );
  }

  @Test
  public void test_getInt_on_SMALLINT_getsIt() throws InvalidAccessException {
    final SqlAccessor uut1 =
        new TypeConvertingSqlAccessor( new SmallIntStubAccessor( (short) 32767 ) );
    assertThat( uut1.getInt( 0 ), equalTo( 32767 ) );
    final SqlAccessor uut2 =
        new TypeConvertingSqlAccessor( new SmallIntStubAccessor( (short) -32768 ) );
    assertThat( uut2.getInt( 0 ), equalTo( -32768 ) );
  }

  @Test
  public void test_getInt_on_INTEGER_getsIt() throws InvalidAccessException {
    final SqlAccessor uut1 =
        new TypeConvertingSqlAccessor( new IntegerStubAccessor( 2147483647 ) );
    assertThat( uut1.getInt( 0 ), equalTo( 2147483647 ) );
    final SqlAccessor uut2 =
        new TypeConvertingSqlAccessor( new IntegerStubAccessor( -2147483648 ) );
    assertThat( uut2.getInt( 0 ), equalTo( -2147483648 ) );
  }

  @Test
  public void test_getInt_on_BIGINT_thatFits_getsIt() throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new BigIntStubAccessor( 2147483647 ) );
    assertThat( uut.getInt( 0 ), equalTo( 2147483647 ) );
  }

  @Test( expected = SQLConversionOverflowException.class )
  public void test_getInt_on_BIGINT_thatOverflows_throws() throws
  InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new BigIntStubAccessor( 2147483648L ) );
    try {
      uut.getInt( 0 );
    }
    catch ( Throwable e ) {
      assertThat( e.getMessage(), containsString( "2147483648" ) );
      assertThat( e.getMessage(), containsString( "getInt" ) );
      assertThat( e.getMessage(), allOf( containsString( "long" ),
                                         containsString( "BIGINT" ) ) );
      throw e;
    }
  }

  @Test
  public void test_getInt_on_FLOAT_thatFits_getsIt() throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new FloatStubAccessor( 1e9f ) );
    assertThat( uut.getInt( 0 ), equalTo( 1_000_000_000 ) );
  }

  @Test( expected = SQLConversionOverflowException.class )
  public void test_getInt_on_FLOAT_thatOverflows_rejectsIt() throws
  InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new FloatStubAccessor( 1e10f ) );
    try {
      uut.getInt( 0 );
    }
    catch ( Throwable e ) {
      assertThat( e.getMessage(), containsString( "1.0E10" ) );
      assertThat( e.getMessage(), containsString( "getInt" ) );
      assertThat( e.getMessage(), allOf( containsString( "float" ),
                                         anyOf( containsString( "REAL" ),
                                                containsString( "FLOAT" ) ) ) );
      throw e;
    }
  }

  @Test
  public void test_getInt_on_DOUBLE_thatFits_getsIt() throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new DoubleStubAccessor( -2147483648.0d ) );
    assertThat( uut.getInt( 0 ), equalTo( -2147483648 ) );
  }

  @Test( expected = SQLConversionOverflowException.class )
  public void test_getInt_on_DOUBLE_thatOverflows_rejectsIt() throws
  InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new DoubleStubAccessor( -2147483649.0d ) );
    try {
      uut.getInt( 0 );
    }
    catch ( Throwable e ) {
      assertThat( e.getMessage(), containsString( "-2.147483649E9" ) );
      assertThat( e.getMessage(), containsString( "getInt" ) );
      assertThat( e.getMessage(), allOf( containsString( "double" ),
                                         containsString( "DOUBLE PRECISION" ) ) );
      throw e;
    }
  }


  ////////////////////////////////////////
  // - getLong:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;

  @Test
  public void test_getLong_on_TINYINT_getsIt() throws InvalidAccessException {
    final SqlAccessor uut1 =
        new TypeConvertingSqlAccessor( new TinyIntStubAccessor( (byte) 127 ) );
    assertThat( uut1.getLong( 0 ), equalTo( 127L ) );
    final SqlAccessor uut2 =
        new TypeConvertingSqlAccessor( new TinyIntStubAccessor( (byte) -128 ) );
    assertThat( uut2.getLong( 0 ), equalTo( -128L ) );
  }

  @Test
  public void test_getLong_on_SMALLINT_getsIt() throws InvalidAccessException {
    final SqlAccessor uut1 =
        new TypeConvertingSqlAccessor( new SmallIntStubAccessor( (short) 32767 ) );
    assertThat( uut1.getLong( 0 ), equalTo( 32767L ) );
    final SqlAccessor uut2 =
        new TypeConvertingSqlAccessor( new SmallIntStubAccessor( (short) -32768 ) );
    assertThat( uut2.getLong( 0 ), equalTo( -32768L ) );
  }

  @Test
  public void test_getLong_on_INTEGER_getsIt() throws InvalidAccessException {
    final SqlAccessor uut1 =
        new TypeConvertingSqlAccessor( new IntegerStubAccessor( 2147483647 ) );
    assertThat( uut1.getLong( 0 ), equalTo( 2147483647L ) );
    final SqlAccessor uut2 =
        new TypeConvertingSqlAccessor( new IntegerStubAccessor( -2147483648 ) );
    assertThat( uut2.getLong( 0 ), equalTo( -2147483648L ) );
  }

  @Test
  public void test_getLong_on_BIGINT_getsIt() throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new BigIntStubAccessor( 2147483648L ) );
    assertThat( uut.getLong( 0 ), equalTo( 2147483648L ) );
  }

  @Test
  public void test_getLong_on_FLOAT_thatFits_getsIt() throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor(
            new FloatStubAccessor( 9223372036854775807L * 1.0f ) );
    assertThat( uut.getLong( 0 ), equalTo( 9223372036854775807L ) );
  }

  @Test( expected = SQLConversionOverflowException.class )
  public void test_getLong_on_FLOAT_thatOverflows_rejectsIt()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new FloatStubAccessor( 1.5e20f ) );
    try {
      uut.getLong( 0 );
    }
    catch ( Throwable e ) {
      assertThat( e.getMessage(), containsString( "1.5000" ) );
      assertThat( e.getMessage(), containsString( "getLong" ) );
      assertThat( e.getMessage(), allOf( containsString( "float" ),
                                         anyOf( containsString( "REAL" ),
                                                containsString( "FLOAT" ) ) ) );
      throw e;
    }
  }

  @Test
  public void test_getLong_on_DOUBLE_thatFits_getsIt() throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor(
            new DoubleStubAccessor( 9223372036854775807L * 1.0d ) );
    assertThat( uut.getLong( 0 ), equalTo( 9223372036854775807L ) );
  }

  @Test( expected = SQLConversionOverflowException.class )
  public void test_getLong_on_DOUBLE_thatOverflows_rejectsIt()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new DoubleStubAccessor( 1e20 ) );
    try {
      uut.getLong( 0 );
    }
    catch ( Throwable e ) {
      assertThat( e.getMessage(), containsString( "1.0E20" ) );
      assertThat( e.getMessage(), containsString( "getLong" ) );
      assertThat( e.getMessage(), allOf( containsString( "double" ),
                                         containsString( "DOUBLE PRECISION" ) ) );
      throw e;
    }
  }



  ////////////////////////////////////////
  // - getFloat:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;

  @Test
  public void test_getFloat_on_FLOAT_getsIt() throws InvalidAccessException {
    final SqlAccessor uut1 =
        new TypeConvertingSqlAccessor( new FloatStubAccessor( 1.23f ) );
    assertThat( uut1.getFloat( 0 ), equalTo( 1.23f ) );
    final SqlAccessor uut2 =
        new TypeConvertingSqlAccessor( new FloatStubAccessor( Float.MAX_VALUE ) );
    assertThat( uut2.getFloat( 0 ), equalTo( Float.MAX_VALUE ) );
    final SqlAccessor uut3 =
        new TypeConvertingSqlAccessor( new FloatStubAccessor( Float.MIN_VALUE ) );
    assertThat( uut3.getFloat( 0 ), equalTo( Float.MIN_VALUE ) );
  }

  @Test
  public void test_getFloat_on_DOUBLE_thatFits_getsIt() throws InvalidAccessException {
    final SqlAccessor uut1 =
        new TypeConvertingSqlAccessor( new DoubleStubAccessor( 1.125 ) );
    assertThat( uut1.getFloat( 0 ), equalTo( 1.125f ) );
    final SqlAccessor uut2 =
        new TypeConvertingSqlAccessor( new DoubleStubAccessor( Float.MAX_VALUE ) );
    assertThat( uut2.getFloat( 0 ), equalTo( Float.MAX_VALUE ) );
  }

  @Test( expected = SQLConversionOverflowException.class )
  public void test_getFloat_on_DOUBLE_thatOverflows_throws()
      throws InvalidAccessException {
    final SqlAccessor uut =
        new TypeConvertingSqlAccessor( new DoubleStubAccessor( 1e100 ) );
    try {
      uut.getFloat( 0 );
    }
    catch ( Throwable e ) {
      assertThat( e.getMessage(), containsString( "1.0E100" ) );
      assertThat( e.getMessage(), containsString( "getFloat" ) );
      assertThat( e.getMessage(), allOf( containsString( "double" ),
                                         anyOf ( containsString( "DOUBLE PRECISION" ),
                                                 containsString( "FLOAT" ) ) ) );
      throw e;
    }
  }


  ////////////////////////////////////////
  // - getDouble:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;

  @Test
  public void test_getDouble_on_FLOAT_getsIt() throws InvalidAccessException {
    final SqlAccessor uut1 =
        new TypeConvertingSqlAccessor( new FloatStubAccessor( 6.02e23f ) );
    assertThat( uut1.getDouble( 0 ), equalTo( (double) 6.02e23f ) );
    final SqlAccessor uut2 =
        new TypeConvertingSqlAccessor( new FloatStubAccessor( Float.MAX_VALUE ) );
    assertThat( uut2.getDouble( 0 ), equalTo( (double) Float.MAX_VALUE ) );
    final SqlAccessor uut3 =
        new TypeConvertingSqlAccessor( new FloatStubAccessor( Float.MIN_VALUE ) );
    assertThat( uut3.getDouble( 0 ), equalTo( (double) Float.MIN_VALUE ) );
  }

  @Test
  public void test_getDouble_on_DOUBLE_getsIt() throws InvalidAccessException {
    final SqlAccessor uut1 =
        new TypeConvertingSqlAccessor( new DoubleStubAccessor( -1e100 ) );
    assertThat( uut1.getDouble( 0 ), equalTo( -1e100  ) );
    final SqlAccessor uut2 =
        new TypeConvertingSqlAccessor( new DoubleStubAccessor( Double.MAX_VALUE ) );
    assertThat( uut2.getDouble( 0 ), equalTo( Double.MAX_VALUE ) );
    final SqlAccessor uut3 =
        new TypeConvertingSqlAccessor( new DoubleStubAccessor( Double.MIN_VALUE ) );
    assertThat( uut3.getDouble( 0 ), equalTo( Double.MIN_VALUE ) );
  }

  ////////////////////////////////////////
  // - getBigDecimal:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;

  ////////////////////////////////////////
  // - getBoolean:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;

  ////////////////////////////////////////
  // - getString:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - NCHAR, NVARCHAR, LONGNVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - DATE, TIME, TIMESTAMP;
  //   - DATALINK;

  ////////////////////////////////////////
  // - getNString:
  //   - TINYINT, SMALLINT, INTEGER, BIGINT; REAL, FLOAT, DOUBLE; DECIMAL, NUMERIC;
  //   - BIT, BOOLEAN;
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - NCHAR, NVARCHAR, LONGNVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - DATE, TIME, TIMESTAMP;
  //   - DATALINK;

  ////////////////////////////////////////
  // - getBytes:
  //   - BINARY, VARBINARY, LONGVARBINARY;

  ////////////////////////////////////////
  // - getDate:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - DATE, TIMESTAMP;

  ////////////////////////////////////////
  // - getTime:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - TIME, TIMESTAMP;

  ////////////////////////////////////////
  // - getTimestamp:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - DATE, TIME, TIMESTAMP;

  ////////////////////////////////////////
  // - getAsciiStream:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - CLOB, NCLOB;

  ////////////////////////////////////////
  // - getBinaryStream:
  //   - BINARY, VARBINARY, LONGVARBINARY;

  ////////////////////////////////////////
  // - getCharacterStream:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - NCHAR, NVARCHAR, LONGNVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - CLOB, NCLOB;
  //   - SQLXML;

  ////////////////////////////////////////
  // - getNCharacterStream:
  //   - CHAR, VARCHAR, LONGVARCHAR;
  //   - NCHAR, NVARCHAR, LONGNVARCHAR;
  //   - BINARY, VARBINARY, LONGVARBINARY;
  //   - CLOB, NCLOB;
  //   - SQLXML;

  ////////////////////////////////////////
  // - getClob:
  //   - CLOB, NCLOB;

  ////////////////////////////////////////
  // - getNClob:
  //   - CLOB, NCLOB;

  ////////////////////////////////////////
  // - getBlob:
  //   - BLOB;

  ////////////////////////////////////////
  // - getArray:
  //   - ARRAY;

  ////////////////////////////////////////
  // - getRef:
  //   - REF;

  ////////////////////////////////////////
  // - getURL:
  //   - DATALINK;

  ////////////////////////////////////////
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

  ////////////////////////////////////////
  // - getRowId:
  //   - ROWID;

  ////////////////////////////////////////
  // - getSQLXML:
  //   - SQLXML SQLXML

}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.jdbc.converter;

import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleResultSetMetaData;
import oracle.jdbc.OracleTypes;
import oracle.sql.INTERVALDS;
import oracle.sql.INTERVALYM;
import org.junit.Test;

import java.math.BigInteger;
import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OracleValueConverterTest {
  private OracleValueConverter stringOracleValueConverter =
          new OracleValueConverter(OracleValueConverter.IntervalHandlingMode.STRING);
  private OracleValueConverter numericOracleValueConverter =
          new OracleValueConverter(OracleValueConverter.IntervalHandlingMode.NUMERIC);

  @Test
  public void testExtract() throws Exception {
    OracleResultSet mockOracleResultSet = mock(OracleResultSet.class);
    OracleResultSetMetaData mockMetaData = mock(OracleResultSetMetaData.class);
    final int columnIndex = 0;
    final INTERVALDS intervalds = new INTERVALDS();
    when(mockMetaData.getColumnType(columnIndex)).thenReturn(OracleTypes.INTERVALDS);
    when(mockOracleResultSet.getINTERVALDS(columnIndex)).thenReturn(intervalds);
    assertEquals(intervalds, numericOracleValueConverter.extract(
            mockOracleResultSet, mockMetaData, columnIndex, 0, null, null, null));
  }

  @Test
  public void testConvertCharColumnType() throws Exception {
    String varchar = "varchar";
    assertEquals(varchar, stringOracleValueConverter.convert(varchar, Types.VARCHAR, null, null));
  }

  @Test
  public void testConvertTimestamptzColumnType() throws Exception {
    int value = 1;
    assertEquals(value, stringOracleValueConverter.convert(value, OracleTypes.TIMESTAMPTZ, null, null));
  }

  @Test
  public void testConvertIntervalColumnType() throws Exception {
    final INTERVALYM intervalym = new INTERVALYM("2022-10");
    assertEquals("2022-10", stringOracleValueConverter.convert(intervalym, OracleTypes.INTERVALYM, null, null));
  }

  @Test
  public void testConvertBinaryFloatColumnType() throws Exception {
    final Object value = 1;
    assertEquals(value, stringOracleValueConverter.convert(value, OracleTypes.BINARY_FLOAT, null, null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConvertIntervalNullMode() {
    final OracleValueConverter nullOracleValueConverter =
            new OracleValueConverter(OracleValueConverter.IntervalHandlingMode.parse("test"));
    final INTERVALYM intervalym = new INTERVALYM("2022-10");
    nullOracleValueConverter.convertInterval(intervalym);
  }

  @Test
  public void testConvertIntervalString() {
    final INTERVALYM intervalym = new INTERVALYM("2022-10");
    assertEquals("2022-10", stringOracleValueConverter.convertInterval(intervalym));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConvertIntervalWrongInterval() {
    numericOracleValueConverter.convertInterval("test");
  }

  @Test
  public void testConvertIntervalYM() {
    final INTERVALYM intervalym = new INTERVALYM("2022-10");
    assertEquals(BigInteger.valueOf(63_835_689_600_000_000L), numericOracleValueConverter.convertInterval(intervalym));
  }

  @Test
  public void testConvertIntervalDS() {
    final INTERVALDS intervalds = new INTERVALDS("1 2:34:56.789");
    assertEquals(BigInteger.valueOf(95_696_789_000L), numericOracleValueConverter.convertInterval(intervalds));
  }

  @Test
  public void testIntervalHandlingModeParseNull() {
    assertNull(OracleValueConverter.IntervalHandlingMode.parse(null));
  }

  @Test
  public void testIntervalHandlingModeParseDifferentValues() {
    assertNull(OracleValueConverter.IntervalHandlingMode.parse("test"));
  }

  @Test
  public void testIntervalHandlingModeParseNumeric() {
    assertEquals(OracleValueConverter.IntervalHandlingMode.NUMERIC, OracleValueConverter.IntervalHandlingMode.parse("numeric"));
  }

  @Test
  public void testIntervalHandlingModeParseString() {
    assertEquals(OracleValueConverter.IntervalHandlingMode.STRING, OracleValueConverter.IntervalHandlingMode.parse("string"));
  }

  @Test
  public void testConvertNumRightToFloatingPointToMicroLessThanMicroLength() {
    assertEquals("123000", numericOracleValueConverter.convertNumRightToFloatingPointToMicro("123"));
  }

  @Test
  public void testConvertNumRightToFloatingPointToMicroOverMicroLength() {
    assertEquals("123456", numericOracleValueConverter.convertNumRightToFloatingPointToMicro("1234567"));
  }

  @Test
  public void testConvertNumRightToFloatingPointToMicroSameAsMicroLength() {
    assertEquals("123456", numericOracleValueConverter.convertNumRightToFloatingPointToMicro("123456"));
  }
}

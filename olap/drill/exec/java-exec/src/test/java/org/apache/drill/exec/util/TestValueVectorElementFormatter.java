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
package org.apache.drill.exec.util;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.test.BaseTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

public class TestValueVectorElementFormatter extends BaseTest {

  @Mock
  private OptionManager options;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testFormatValueVectorElementTimestampEmptyPattern() {
    when(options.getString(ExecConstants.WEB_DISPLAY_FORMAT_TIMESTAMP)).thenReturn("");
    ValueVectorElementFormatter formatter = new ValueVectorElementFormatter(options);
    String formattedValue = formatter.format(
        LocalDateTime.of(2012, 11, 5, 13, 0, 30, 120000000),
        TypeProtos.MinorType.TIMESTAMP);
    assertEquals("2012-11-05T13:00:30.120", formattedValue);
  }

  @Test
  public void testFormatValueVectorElementTimestampValidPattern() {
    when(options.getString(ExecConstants.WEB_DISPLAY_FORMAT_TIMESTAMP)).thenReturn("yyyy-MM-dd HH:mm:ss.SS");
    ValueVectorElementFormatter formatter = new ValueVectorElementFormatter(options);
    String formattedValue = formatter.format(
        LocalDateTime.of(2012, 11, 5, 13, 0, 30, 120000000),
        TypeProtos.MinorType.TIMESTAMP);
    assertEquals("2012-11-05 13:00:30.12", formattedValue);
  }

  @Test
  public void testFormatValueVectorElementDateEmptyPattern() {
    when(options.getString(ExecConstants.WEB_DISPLAY_FORMAT_DATE)).thenReturn("");
    ValueVectorElementFormatter formatter = new ValueVectorElementFormatter(options);
    String formattedValue = formatter.format(
        LocalDate.of(2012, 11, 5),
        TypeProtos.MinorType.DATE);
    assertEquals("2012-11-05", formattedValue);
  }

  @Test
  public void testFormatValueVectorElementDateValidPattern() {
    when(options.getString(ExecConstants.WEB_DISPLAY_FORMAT_DATE)).thenReturn("EEE, MMM d, yyyy");
    ValueVectorElementFormatter formatter = new ValueVectorElementFormatter(options);
    String formattedValue = formatter.format(
        LocalDate.of(2012, 11, 5),
        TypeProtos.MinorType.DATE);
    assertEquals("Mon, Nov 5, 2012", formattedValue);
  }

  @Test
  public void testFormatValueVectorElementDateUnsupportedPattern() {
    when(options.getString(ExecConstants.WEB_DISPLAY_FORMAT_DATE)).thenReturn("yyyy-MM-dd HH:mm:ss.SS");
    ValueVectorElementFormatter formatter = new ValueVectorElementFormatter(options);
    String formattedValue = formatter.format(
        LocalDate.of(2012, 11, 5),
        TypeProtos.MinorType.DATE);
    assertNull(formattedValue);
  }

  @Test
  public void testFormatValueVectorElementTimeEmptyPattern() {
    when(options.getString(ExecConstants.WEB_DISPLAY_FORMAT_TIME)).thenReturn("");
    ValueVectorElementFormatter formatter = new ValueVectorElementFormatter(options);
    String formattedValue = formatter.format(
        LocalTime.of(13, 0, 30, 120000000),
        TypeProtos.MinorType.TIME);
    assertEquals("13:00:30.120", formattedValue);
  }

  @Test
  public void testFormatValueVectorElementTimeValidPattern() {
    when(options.getString(ExecConstants.WEB_DISPLAY_FORMAT_TIME)).thenReturn("h:mm:ss a");
    ValueVectorElementFormatter formatter = new ValueVectorElementFormatter(options);
    String formattedValue = formatter.format(
        LocalTime.of(13, 0, 30),
        TypeProtos.MinorType.TIME);
    assertEquals("1:00:30 PM", formattedValue);
  }

  @Test
  public void testFormatValueVectorElementTimeUnsupportedPattern() {
    when(options.getString(ExecConstants.WEB_DISPLAY_FORMAT_TIME)).thenReturn("yyyy-MM-dd HH:mm:ss.SS");
    ValueVectorElementFormatter formatter = new ValueVectorElementFormatter(options);
    String formattedValue = formatter.format(
        LocalTime.of(13, 0, 30),
        TypeProtos.MinorType.TIME);
    assertNull(formattedValue);
  }

  @Test
  public void testFormatValueVectorElementAllDateTimeFormats() {
    when(options.getString(ExecConstants.WEB_DISPLAY_FORMAT_TIMESTAMP)).thenReturn("yyyy-MM-dd HH:mm:ss.SS");
    when(options.getString(ExecConstants.WEB_DISPLAY_FORMAT_DATE)).thenReturn("EEE, MMM d, yyyy");
    when(options.getString(ExecConstants.WEB_DISPLAY_FORMAT_TIME)).thenReturn("h:mm:ss a");
    ValueVectorElementFormatter formatter = new ValueVectorElementFormatter(options);
    String formattedTimestamp = formatter.format(
        LocalDateTime.of(2012, 11, 5, 13, 0, 30, 120000000),
        TypeProtos.MinorType.TIMESTAMP);
    String formattedDate = formatter.format(
        LocalDate.of(2012, 11, 5),
        TypeProtos.MinorType.DATE);
    String formattedTime = formatter.format(
        LocalTime.of(13, 0, 30),
        TypeProtos.MinorType.TIME);
    assertEquals("2012-11-05 13:00:30.12", formattedTimestamp);
    assertEquals("Mon, Nov 5, 2012", formattedDate);
    assertEquals("1:00:30 PM", formattedTime);
  }

  @Test // DRILL-7049
  public void testFormatValueVectorElementBinary() {
    ValueVectorElementFormatter formatter = new ValueVectorElementFormatter(options);
    String testString = "Fred";
    String formattedValue = formatter.format(
            testString.getBytes(StandardCharsets.UTF_8),
            TypeProtos.MinorType.VARBINARY);
    assertEquals("Wrong Varbinary value formatting", testString, formattedValue);
  }
}

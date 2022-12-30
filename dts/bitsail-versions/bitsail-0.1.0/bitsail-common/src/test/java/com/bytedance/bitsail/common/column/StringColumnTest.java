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

package com.bytedance.bitsail.common.column;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;

import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class StringColumnTest {

  private String timeZone;

  @Before
  public void before() {
    timeZone = ZoneOffset.ofHours(0).getId();
    BitSailConfiguration bitSailConfiguration = BitSailConfiguration.newDefault();
    bitSailConfiguration.set(CommonOptions.DateFormatOptions.TIME_ZONE, timeZone);
    ColumnCast.refresh();
    ColumnCast.initColumnCast(bitSailConfiguration);
  }

  @Test
  public void asDate() {
    String timeStr = "2019-04-01 11:11:11";
    StringColumn strColumn = new StringColumn(timeStr);
    Date dateTime = strColumn.asDate();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    String dateStr = formatter.format(dateTime
        .toInstant()
        .atZone(ZoneId.of(timeZone)).toOffsetDateTime());
    assertEquals(dateStr, timeStr);
  }

  @Test
  public void testAsBytes() {
    String str = "abc";
    StringColumn sc = new StringColumn(str);
    assertEquals(str, new String(sc.asBytes()));

    str = "";
    sc = new StringColumn(str);
    assertEquals(str, new String(sc.asBytes()));

  }
}
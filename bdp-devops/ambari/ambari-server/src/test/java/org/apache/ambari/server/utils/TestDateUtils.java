/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

import junit.framework.Assert;

public class TestDateUtils {

  @Test
  public void testConvertToReadableTime() throws Exception {
    Long timestamp = 1389125737000L;
    String readableTime = DateUtils.convertToReadableTime(timestamp);

    // expected format with tz set to utc
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

    //parse using formatter with TZ=local and format using above formatter with TZ=UTC
    Assert.assertEquals("2014-01-07 20:15:37",
        sdf.format(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(readableTime)));
  }

  @Test
  public void testConvertToDate() throws Exception {
    String time = "2013-11-18T14:29:29-0000";
    Date date = DateUtils.convertToDate(time);
    Assert.assertNotNull(date);
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
    calendar.set(Calendar.YEAR, 2013);
    calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
    calendar.set(Calendar.DAY_OF_MONTH, 18);
    calendar.set(Calendar.HOUR_OF_DAY, 14);
    calendar.set(Calendar.MINUTE, 29);
    calendar.set(Calendar.SECOND, 29);
    calendar.set(Calendar.MILLISECOND, 0);
    Assert.assertEquals(0, date.compareTo(calendar.getTime()));
  }

  @Test
  public void testGetDateDifferenceInMinutes() throws Exception {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
    calendar.set(Calendar.YEAR, 2013);
    calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
    calendar.set(Calendar.DAY_OF_MONTH, 18);
    calendar.set(Calendar.HOUR_OF_DAY, 14);
    calendar.set(Calendar.MINUTE, 49);
    calendar.set(Calendar.SECOND, 29);
    calendar.set(Calendar.MILLISECOND, 0);
    String time = "2013-11-18T14:29:29-0000";
    Date date = DateUtils.convertToDate(time);
    Long diff = (Math.abs(date.getTime() - calendar.getTimeInMillis())) / (60 * 1000) % 60;
    Assert.assertEquals(Long.valueOf(20L).longValue(), diff.longValue());
  }
}

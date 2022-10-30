/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corebase.utils;

import java.time.format.DateTimeParseException;
import java.util.Date;
import org.junit.Assert;
import org.junit.Test;

public class DateTimeConvertUtilsTest {
    @Test
    public void testDateTimeFormatUtils() {

        // test yyyyMMddHHmmss string to ms
        String strDateTime11 = "20220114155900";
        long timeStamp11 = DateTimeConvertUtils.yyyyMMddHHmmss2ms(strDateTime11);
        String newDateTime11 = DateTimeConvertUtils.ms2yyyyMMddHHmmss(timeStamp11);
        Assert.assertEquals(strDateTime11, newDateTime11);
        // test yyyyMMddHHmm string to ms 2
        String strDateTime12 = "202201141559";
        long timeStamp12 = DateTimeConvertUtils.yyyyMMddHHmm2ms(strDateTime12);
        String newDateTime12 = DateTimeConvertUtils.ms2yyyyMMddHHmm(timeStamp12);
        Assert.assertEquals(strDateTime12, newDateTime12);
        // test yyyyMMddHHmmss string to ms 3
        long timeStamp13 = DateTimeConvertUtils.yyyyMMddHHmmss2ms(strDateTime11);
        String newDateTime13 = DateTimeConvertUtils.ms2yyyyMMddHHmm(timeStamp13);
        Assert.assertEquals(strDateTime12, newDateTime13);

        // test date to string
        Date curDate21 = new Date();
        String strTime21 = DateTimeConvertUtils.date2yyyyMMddHHmmss(curDate21);
        long timeStamp21 = DateTimeConvertUtils.yyyyMMddHHmmss2ms(strTime21);
        Date newDate21 = new Date(timeStamp21);
        String strNewTime21 = DateTimeConvertUtils.date2yyyyMMddHHmmss(newDate21);
        Assert.assertEquals(strTime21, strNewTime21);

        String orig31 = "20220115152055";
        Date date32 = DateTimeConvertUtils.yyyyMMddHHmmss2date(orig31);
        String strNew33 = DateTimeConvertUtils.date2yyyyMMddHHmmss(date32);
        Assert.assertEquals(orig31, strNew33);

        // test null cases
        String curDate41 = DateTimeConvertUtils.date2yyyyMMddHHmmss(null);
        Assert.assertEquals(curDate41, TStringUtils.EMPTY);
        Date curDate42 = DateTimeConvertUtils.yyyyMMddHHmmss2date(null);
        Assert.assertNull(curDate42);
        Date curDate43 = DateTimeConvertUtils.yyyyMMddHHmmss2date("202201152536");
        Assert.assertNull(curDate43);
        Throwable exVal = null;
        try {
            DateTimeConvertUtils.yyyyMMddHHmm2ms(null);
        } catch (Throwable ex) {
            exVal = ex;
        }
        Assert.assertNotNull(exVal);
        Assert.assertEquals(exVal.getClass(), NullPointerException.class);
        //
        exVal = null;
        try {
            DateTimeConvertUtils.yyyyMMddHHmm2ms("20220115172722");
        } catch (Throwable ex) {
            exVal = ex;
        }
        Assert.assertNotNull(exVal);
        Assert.assertEquals(exVal.getClass(), DateTimeParseException.class);
        Assert.assertTrue(exVal.getMessage().contains("could not be parsed"));
        //
        exVal = null;
        try {
            DateTimeConvertUtils.yyyyMMddHHmm2ms("202201158827");
        } catch (Throwable ex) {
            exVal = ex;
        }
        Assert.assertNotNull(exVal);
        Assert.assertEquals(exVal.getClass(), DateTimeParseException.class);
        Assert.assertTrue(exVal.getMessage().contains("could not be parsed"));
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ranger.common;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("deprecation")
public class TestDateUtil {

        private static final TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT+0");

	@Autowired
	DateUtil dateUtil = new DateUtil();

    @Test
    @Ignore
	public void testGetDateFromNow() {
		int days = 1;		
		Date dateCheck= dateUtil.getDateFromNow(days);
		int minutes=dateCheck.getMinutes();
		int hourse=dateCheck.getHours();
		Assert.assertEquals(dateCheck.getDay(),days+2);
		Assert.assertEquals(dateCheck.getMinutes(), minutes);
		Assert.assertEquals(dateCheck.getHours(), hourse);
	}

    @Test
    public void testDateFromNow(){

	int days = 2;
	int hours = 3;
	int minutes = 50;

	Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, days);
                cal.add(Calendar.HOUR, hours);
                cal.add(Calendar.MINUTE, minutes);
                Date expectedDate = cal.getTime();


	Date actualDate = dateUtil.getDateFromNow(days, hours, minutes);

	Assert.assertEquals(expectedDate.getYear(), actualDate.getYear());
	Assert.assertEquals(expectedDate.getDay(), actualDate.getDay());
	Assert.assertEquals(expectedDate.getMonth(), actualDate.getMonth());

    }

	@Test
	public void testDateToString() {
		Date date = new Date();
		SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
		String dateFromat = DATE_FORMAT.format(date);
		String dateCheck = DateUtil.dateToString(date, dateFromat);
		Assert.assertEquals(dateCheck,dateFromat);
	}
	
	@Test
	public void testGetDateFromGivenDate(){
		Date date = new Date();
		int days=0;
		int hours=date.getHours();
		int minutes=date.getMinutes();
		int second=date.getSeconds();
		Date currentDate = dateUtil.getDateFromGivenDate(date, days, 0, 0, 0);
		Assert.assertEquals(currentDate.getDay(),date.getDay()+days);
		Assert.assertEquals(currentDate.getHours(),hours);
		Assert.assertEquals(currentDate.getMinutes(),minutes);
		Assert.assertEquals(currentDate.getSeconds(),second);
	}
	
	@Test
	public void testAddTimeOffset(){
		Date date = new Date();
		int mins=date.getMinutes();
		Date currentDate=dateUtil.addTimeOffset(date, 0);
		Assert.assertEquals(currentDate.getDate(),date.getDate());
		Assert.assertEquals(currentDate.getMinutes(),mins);
	}

        @Test
        public void testStringToDate(){
                String dateString = "2018-05-31";
                String dateFormat = "yyyy-MM-dd";

                Calendar cal = Calendar.getInstance();
                                cal.set(2018 - 1900, 04, 31);
                                Date expectedDate = new Date(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DATE));

                Date actualDate = dateUtil.stringToDate(dateString, dateFormat);
                if(actualDate != null){
                        Assert.assertEquals(expectedDate.getYear(), actualDate.getYear());
                                        Assert.assertEquals(expectedDate.getDay(), actualDate.getDay());
                                        Assert.assertEquals(expectedDate.getMonth(), actualDate.getMonth());
                }

        }

        @Test
        public void testGetUTCDate(){

                Calendar local=Calendar.getInstance();
            int offset = local.getTimeZone().getOffset(local.getTimeInMillis());
            GregorianCalendar utc = new GregorianCalendar(gmtTimeZone);
            utc.setTimeInMillis(local.getTimeInMillis());
            utc.add(Calendar.MILLISECOND, -offset);
            Date expectedDate = utc.getTime();

                Date actualDate = dateUtil.getUTCDate();
                Assert.assertEquals(actualDate.getDate(),expectedDate.getDate());
                Assert.assertEquals(actualDate.getMinutes(),expectedDate.getMinutes());
        }

        @Test
        public void testGetUTCDateEpoh(){

                Calendar local=Calendar.getInstance();
            int offset = local.getTimeZone().getOffset(2008);
            GregorianCalendar utc = new GregorianCalendar(gmtTimeZone);
            utc.setTimeInMillis(2008);
            utc.add(Calendar.MILLISECOND, -offset);
            Date expectedDate = utc.getTime();

                Date actualDate = dateUtil.getUTCDate(2008);
                Assert.assertEquals(actualDate.getDate(),expectedDate.getDate());
                Assert.assertEquals(actualDate.getMinutes(),expectedDate.getMinutes());
        }

        @Test
        public void testGetLocalDateForUTCDate(){
                Date dt = new Date();
                Calendar local=Calendar.getInstance();
            int offset = local.getTimeZone().getOffset(local.getTimeInMillis());
            GregorianCalendar utc = new GregorianCalendar(gmtTimeZone);
            utc.setTimeInMillis(dt.getTime());
            utc.add(Calendar.MILLISECOND, offset);
            Date expectedDate = utc.getTime();

                Date actualDate = dateUtil.getLocalDateForUTCDate(dt);
                Assert.assertEquals(actualDate.getDate(),expectedDate.getDate());
                Assert.assertEquals(actualDate.getMinutes(),expectedDate.getMinutes());
                Assert.assertEquals(actualDate.getHours(),expectedDate.getHours());

        }

}

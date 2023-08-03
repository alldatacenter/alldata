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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 /**
 *
 */
package org.apache.ranger.common;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;


@Component
public class DateUtil {

    private static final TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT+0");

    public Date getDateFromNow(int days) {
    	return getDateFromNow(days, 0, 0);
    }

    public Date getDateFromNow(int days, int hours, int minutes) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, days);
		cal.add(Calendar.HOUR, hours);
		cal.add(Calendar.MINUTE, minutes);
		return cal.getTime();
    }

	public static String dateToString(Date date, String dateFromat) {
		SimpleDateFormat formatter = new SimpleDateFormat(dateFromat);
		return formatter.format(date).toString();
	}

	public Date getDateFromGivenDate(Date date, int days, int hours,int minutes, int second) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DATE, days);
		cal.add(Calendar.HOUR, hours);
		cal.add(Calendar.MINUTE, minutes);
		cal.add(Calendar.SECOND, second);
		return cal.getTime();
	}
	/**
	 * useful for converting client time zone Date to UTC Date
	 * @param date
	 * @param mins
	 * @return
	 */
	public Date addTimeOffset(Date date, int mins) {
		if (date == null) {
			return date;
		}
		long t = date.getTime();
		Date newDate = new Date(t + (mins * 60000));
		return newDate;
	}
	
	
	public static Date getUTCDate(){
		try{
			Calendar local=Calendar.getInstance();
		    int offset = local.getTimeZone().getOffset(local.getTimeInMillis());
		    GregorianCalendar utc = new GregorianCalendar(gmtTimeZone);
		    utc.setTimeInMillis(local.getTimeInMillis());
		    utc.add(Calendar.MILLISECOND, -offset);
		    return utc.getTime();
		}catch(Exception ex){
			return null;
		}
	}

	public static Date getUTCDate(long epoh) {
		if(epoh==0){
			return null;
		}
		try{
			Calendar local=Calendar.getInstance();
		    int offset = local.getTimeZone().getOffset(epoh);
		    GregorianCalendar utc = new GregorianCalendar(gmtTimeZone);
		    utc.setTimeInMillis(epoh);
		    utc.add(Calendar.MILLISECOND, -offset);	
		    return utc.getTime();
	    }catch(Exception ex){
	    	return null;
	    }	    		
	}
	public static Date getLocalDateForUTCDate(Date date) {
		Calendar local  = Calendar.getInstance();
		int      offset = local.getTimeZone().getOffset(local.getTimeInMillis());
		GregorianCalendar utc = new GregorianCalendar();
		utc.setTimeInMillis(date.getTime());
		utc.add(Calendar.MILLISECOND, offset);
		return utc.getTime();
	}

	public static Date stringToDate(String dateString, String dateFromat){
		SimpleDateFormat simpleDateFormat = null;
		Date date = null;
		if(!StringUtils.isEmpty(dateString) && !StringUtils.isEmpty(dateFromat)){
			try{
				simpleDateFormat = new SimpleDateFormat(dateFromat);
				date = simpleDateFormat.parse(dateString);
			}catch(Exception ex){
				return null;
			}
		}
	    return date;
	  }
}

/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.utils;

import java.util.Date;
import java.util.Locale;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.tz.FixedDateTimeZone;


public class DateUtils {
    private static final DateTimeZone GMT = new FixedDateTimeZone("GMT", "GMT", 0, 0);
    private static final long MILLI_SECONDS_OF_365_DAYS = 365L*24*60*60*1000;
    /** RFC 822 format */
    protected static final DateTimeFormatter rfc822DateFormat = DateTimeFormat
            .forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withLocale(Locale.US).withZone(GMT);

    /** ISO 8601 format */
    protected static final DateTimeFormatter iso8601DateFormat =
            ISODateTimeFormat.dateTime().withZone(GMT);
    
    /** Alternate ISO 8601 format without fractional seconds */
    protected static final DateTimeFormatter alternateIso8601DateFormat =
        DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(GMT);

    /**
     * Formats the specified date as an RFC 822 string.
     *
     * @param date The date to format.
     *
     * @return The RFC 822 string representing the specified date.
     */
    public static String formatRFC822Date(Date date) {
        return rfc822DateFormat.print(date.getTime());
    }

    /**
     * Parses the specified date string as an RFC 822 date and returns the Date object.
     *
     * @param dateString The date string to parse.
     *
     * @return The parsed Date object.
     */
    public static Date parseRFC822Date(String dateString) {
        return new Date(rfc822DateFormat.parseMillis(dateString));
    }

    /**
     * Formats the specified date as an ISO 8601 string.
     *
     * @param date The date to format.
     *
     * @return The ISO 8601 string representing the specified date.
     */
    public static String formatISO8601Date(Date date) {
        return iso8601DateFormat.print(date.getTime());
    }
    
    /**
     * Parses the specified date string as an ISO 8601 date and returns the Date
     * object.
     *
     * @param dateString
     *            The date string to parse.
     *
     * @return The parsed Date object.
     */
    public static Date parseISO8601Date(String dateString) {

        // For EC2 Spot Fleet.
        if (dateString.endsWith("+0000")) {
            dateString = dateString
                    .substring(0, dateString.length() - 5)
                    .concat("Z");
        }

        String temp = tempDateStringForJodaTime(dateString);
        try {
            if (temp.equals(dateString)) {
                // Normal case: nothing special here
                return new Date(iso8601DateFormat.parseMillis(dateString));
            }
            // Handling edge case:
            // Joda-time can only handle up to year 292278993 but we are given
            // 292278994;  So we parse the date string by first adjusting
            // the year to 292278993. Then we add 1 year back afterwards.
            final long milliLess365Days = iso8601DateFormat.parseMillis(temp);
            final long milli = milliLess365Days + MILLI_SECONDS_OF_365_DAYS;
            if (milli < 0) { // overflow!
                // re-parse the original date string using JodaTime so as to
                // throw  an exception with a consistent message
                return new Date(iso8601DateFormat.parseMillis(dateString));
            }
            return new Date(milli);
        } catch (IllegalArgumentException e) {
            try {
                return new Date(alternateIso8601DateFormat.parseMillis(dateString));
                // If the first ISO 8601 parser didn't work, try the alternate
                // version which doesn't include fractional seconds
            } catch(Exception oops) {
                // no the alternative route doesn't work; let's bubble up the original exception
                throw e;
            }
        }
    }
    
    /**
     * Returns a date string with the prefix temporarily substituted, if
     * applicable, so that JodaTime can handle it.  Otherwise, if not applicable,
     * the original date string is returned.
     */
    private static String tempDateStringForJodaTime(String dateString) {
        final String fromPrefix = "292278994-";
        final String toPrefix   = "292278993-";
        return dateString.startsWith(fromPrefix)
             ? toPrefix + dateString.substring(fromPrefix.length())
             : dateString;
    }
}

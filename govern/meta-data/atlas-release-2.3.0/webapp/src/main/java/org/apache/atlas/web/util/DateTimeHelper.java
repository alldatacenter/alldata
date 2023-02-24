/**
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

package org.apache.atlas.web.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Support function to parse and format date.
 */
public final class DateTimeHelper {

    public static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String DATE_PATTERN =
            "(2\\d\\d\\d|19\\d\\d)-(0[1-9]|1[012])-(0[1-9]|1[0-9]|2[0-9]|3[01])T" + "([0-1][0-9]|2[0-3]):([0-5][0-9])Z";
    private static final Pattern PATTERN = Pattern.compile(DATE_PATTERN);

    private static ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        public DateFormat initialValue() {
            DateFormat dateFormat = new SimpleDateFormat(ISO8601_FORMAT);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return dateFormat;
        }
    };

    private DateTimeHelper() {
    }

    public static DateFormat getDateFormat() {
        return DATE_FORMAT.get();
    }

    public static String formatDateUTC(Date date) {
        return (date != null) ? getDateFormat().format(date) : null;
    }
}
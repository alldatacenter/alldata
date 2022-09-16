/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.sort.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StringUtil {

    /**
     * formatDate
     *
     * @param dateStr String
     * @param inputFormat String
     * @param format String
     * @return String
     */
    public static String formatDate(String dateStr, String inputFormat, String format) {
        String resultStr = dateStr;

        try {
            Date date = (new SimpleDateFormat(inputFormat)).parse(dateStr);
            resultStr = formatDate(date, format);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return resultStr;
    }

    /**
     * formatDate
     *
     * @param dateStr String
     * @param format String
     * @return String
     */
    public static String formatDate(String dateStr, String format) {
        String inputFormat = "yyyy-MM-dd HH:mm:ss";
        if (dateStr == null) {
            return "";
        } else {
            if (dateStr.matches("\\d{1,4}\\-\\d{1,2}\\-\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3}")) {
                inputFormat = "yyyy-MM-dd HH:mm:ss.SSS";
            } else if (dateStr.matches("\\d{4}\\-\\d{1,2}\\-\\d{1,2} +\\d{1,2}:\\d{1,2}")) {
                inputFormat = "yyyy-MM-dd HH:mm:ss";
            } else if (dateStr.matches("\\d{4}\\-\\d{1,2}\\-\\d{1,2} +\\d{1,2}:\\d{1,2}")) {
                inputFormat = "yyyy-MM-dd HH:mm";
            } else if (dateStr.matches("\\d{4}\\-\\d{1,2}\\-\\d{1,2} +\\d{1,2}")) {
                inputFormat = "yyyy-MM-dd HH";
            } else if (dateStr.matches("\\d{4}\\-\\d{1,2}\\-\\d{1,2} +\\d{1,2}")) {
                inputFormat = "yyyy-MM-dd";
            } else if (dateStr.matches("\\d{1,4}/\\d{1,2}/\\d{1,2}\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{1,3}")) {
                inputFormat = "yyyy/MM/dd HH:mm:ss.SSS";
            } else if (dateStr.matches("\\d{4}/\\d{1,2}/\\d{1,2} +\\d{1,2}:\\d{1,2}")) {
                inputFormat = "yyyy/MM/dd HH:mm:ss";
            } else if (dateStr.matches("\\d{4}/\\d{1,2}/\\d{1,2} +\\d{1,2}:\\d{1,2}")) {
                inputFormat = "yyyy/MM/dd HH:mm";
            } else if (dateStr.matches("\\d{4}/\\d{1,2}/\\d{1,2} +\\d{1,2}")) {
                inputFormat = "yyyy/MM/dd HH";
            } else if (dateStr.matches("\\d{4}/\\d{1,2}/\\d{1,2} +\\d{1,2}")) {
                inputFormat = "yyyy/MM/dd";
            }

            String resultStr = formatDate(dateStr, inputFormat, format);
            return resultStr;
        }
    }

    /**
     * formatDate
     *
     * @param date Data
     * @param format String
     * @return String
     */
    public static String formatDate(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    /**
     * use default time foramt
     *
     * @param date {@link Date}
     * @return String
     */
    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

}

/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.projectmanager.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    private static final Logger LOG = LoggerFactory.getLogger(DateUtils.class);

    private static final String FORMAT_HH_T_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String FORMAT_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    private static final String FORMAT_HH_MM_SS_S = "yyyy-MM-dd HH:mm:ss.S";
    private static final String FORMAT_HH_MM = "yyyy-MM-dd HH:mm";


    /**
     * contain T,Z format date time convert
     *
     * @param dateTime
     * @return
     * @throws Exception
     */
    public static String dateTimeTZConvert(String dateTime) throws Exception {
        Date date = new SimpleDateFormat(FORMAT_HH_T_Z).parse(dateTime);
        String time = new SimpleDateFormat(FORMAT_HH_MM_SS).format(date);
        return time;
    }

    /**
     * Time format to be reserved to minutes.
     * yyyy-MM-dd HH:mm:ss.S to yyyy-MM-dd HH:mm
     *
     * @param dateTime the amount of time needed to process
     * @return string date time
     */
    public static String timeFormatReservedMinutes(String dateTime) {
        String timeMin = "";
        try {
            SimpleDateFormat sdfSec = new SimpleDateFormat(FORMAT_HH_MM_SS_S);
            Date dt = sdfSec.parse(dateTime);
            SimpleDateFormat sdfMin = new SimpleDateFormat(FORMAT_HH_MM_SS);
            timeMin = sdfMin.format(dt);
        } catch (Exception e) {
            LOG.error("timeFormatReservedMinutes parse error:{}", e);
        }
        return timeMin;
    }

    /**
     * Time format to be reserved to second.
     * yyyy-MM-dd HH:mm:ss.S to yyyy-MM-dd HH:mm:ss
     *
     * @param dateTime the amount of time needed to process
     * @return string date time
     */
    public static String timeFormatReservedSecond(String dateTime) {
        String timeSec = "";
        try {
            SimpleDateFormat sdfSec = new SimpleDateFormat(FORMAT_HH_MM_SS_S);
            Date dt = sdfSec.parse(dateTime);
            SimpleDateFormat sdfMin = new SimpleDateFormat(FORMAT_HH_MM_SS);
            timeSec = sdfMin.format(dt);
        } catch (Exception e) {
            LOG.error("timeFormatReservedMinutes parse error:{}", e);
        }
        return timeSec;
    }
}

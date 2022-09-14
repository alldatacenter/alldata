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

package org.apache.inlong.manager.common.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * Date utils.
 */
@Slf4j
@UtilityClass
public class DateUtils {

    /**
     * Get the expiration date from the current time.
     *
     * @param afterDays expires after days
     * @return expiration date
     */
    public static Date getExpirationDate(Integer afterDays) {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, afterDays);
        return cal.getTime();
    }

    /**
     * Get valid days from the beginning date to the end date.
     *
     * @param begin begin date
     * @param end due date
     * @return valid days
     */
    public static Integer getValidDays(Date begin, Date end) {
        ZoneId zoneId = ZoneId.systemDefault();

        LocalDate createLocalDate = begin.toInstant().atZone(zoneId).toLocalDate();
        LocalDate dueLocalDate = end.toInstant().atZone(zoneId).toLocalDate();

        return Math.toIntExact(dueLocalDate.toEpochDay() - createLocalDate.toEpochDay());
    }

}

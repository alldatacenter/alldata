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

package org.apache.ranger.plugin.policyevaluator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerValidityRecurrence;
import org.apache.ranger.plugin.model.RangerValiditySchedule;
import org.apache.ranger.plugin.resourcematcher.ScheduledTimeAlwaysMatcher;
import org.apache.ranger.plugin.resourcematcher.ScheduledTimeExactMatcher;
import org.apache.ranger.plugin.resourcematcher.ScheduledTimeMatcher;
import org.apache.ranger.plugin.resourcematcher.ScheduledTimeRangeMatcher;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class RangerValidityScheduleEvaluator {
    private static final Logger LOG = LoggerFactory.getLogger(RangerValidityScheduleEvaluator.class);
    private static final Logger PERF_LOG = LoggerFactory.getLogger("test.perf.RangerValidityScheduleEvaluator");

    private final static TimeZone defaultTZ = TimeZone.getDefault();

    private static final ThreadLocal<DateFormat> DATE_FORMATTER = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat(RangerValiditySchedule.VALIDITY_SCHEDULE_DATE_STRING_SPECIFICATION);
        }
    };

    private final Date                            startTime;
    private final Date                            endTime;
    private final String                          timeZone;
    private final List<RangerRecurrenceEvaluator> recurrenceEvaluators = new ArrayList<>();

    public RangerValidityScheduleEvaluator(@Nonnull RangerValiditySchedule validitySchedule) {
        this(validitySchedule.getStartTime(), validitySchedule.getEndTime(), validitySchedule.getTimeZone(), validitySchedule.getRecurrences());
    }

    public RangerValidityScheduleEvaluator(String startTimeStr, String endTimeStr, String timeZone, List<RangerValidityRecurrence> recurrences) {
        Date startTime = null;
        Date endTime   = null;

        if (StringUtils.isNotEmpty(startTimeStr)) {
            try {
                startTime = DATE_FORMATTER.get().parse(startTimeStr);
            } catch (ParseException exception) {
                LOG.error("Error parsing startTime:[" + startTimeStr + "]", exception);
            }
        }

        if (StringUtils.isNotEmpty(endTimeStr)) {
            try {
                endTime = DATE_FORMATTER.get().parse(endTimeStr);
            } catch (ParseException exception) {
                LOG.error("Error parsing endTime:[" + endTimeStr + "]", exception);
            }
        }

        this.startTime = startTime;
        this.endTime   = endTime;
        this.timeZone  = timeZone;

        if (CollectionUtils.isNotEmpty(recurrences)) {
            for (RangerValidityRecurrence recurrence : recurrences) {
                recurrenceEvaluators.add(new RangerRecurrenceEvaluator(recurrence));
            }
        }
    }

    public boolean isApplicable(long accessTime) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("===> isApplicable(accessTime=" + accessTime + ")");
        }

        boolean          ret  = false;
        RangerPerfTracer perf = null;

        if (RangerPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_LOG, "RangerValidityScheduleEvaluator.isApplicable(accessTime=" + accessTime + ")");
        }

        long startTimeInMSs = startTime == null ? 0 : startTime.getTime();
        long endTimeInMSs   = endTime == null ? 0 : endTime.getTime();

        if (StringUtils.isNotBlank(timeZone)) {
            TimeZone targetTZ = TimeZone.getTimeZone(timeZone);

            if (startTimeInMSs > 0) {
                startTimeInMSs = getAdjustedTime(startTimeInMSs, targetTZ);
            }

            if (endTimeInMSs > 0) {
                endTimeInMSs = getAdjustedTime(endTimeInMSs, targetTZ);
            }
        }

        if ((startTimeInMSs == 0 || accessTime >= startTimeInMSs) && (endTimeInMSs == 0 || accessTime <= endTimeInMSs)) {
            if (CollectionUtils.isEmpty(recurrenceEvaluators)) {
                ret = true;
            } else {
                Calendar now = new GregorianCalendar();
                now.setTime(new Date(accessTime));

                for (RangerRecurrenceEvaluator recurrenceEvaluator : recurrenceEvaluators) {
                    ret = recurrenceEvaluator.isApplicable(now);

                    if (ret) {
                        break;
                    }
                }
            }
        }

        RangerPerfTracer.log(perf);

	    if (LOG.isDebugEnabled()) {
		    LOG.debug("<=== isApplicable(accessTime=" + accessTime + ") :" + ret);
	    }
        return ret;
    }

    public static long getAdjustedTime(long localTime, TimeZone timeZone) {
        long ret = localTime;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Input:[" + new Date(localTime) + ", target-timezone" + timeZone + "], default-timezone:[" + defaultTZ + "]");
        }

        if (!defaultTZ.equals(timeZone)) {
            int targetOffset  = timeZone.getOffset(localTime);
            int defaultOffset = defaultTZ.getOffset(localTime);
            int diff          = defaultOffset - targetOffset;

            if (LOG.isDebugEnabled()) {
                LOG.debug("Offset of target-timezone from UTC :[" + targetOffset + "]");
                LOG.debug("Offset of default-timezone from UTC :[" + defaultOffset + "]");
                LOG.debug("Difference between default-timezone and target-timezone :[" + diff + "]");
            }

            ret += diff;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Output:[" + new Date(ret) + "]");
        }

        return ret;
    }

    static class RangerRecurrenceEvaluator {
        private final List<ScheduledTimeMatcher> minutes     = new ArrayList<>();
        private final List<ScheduledTimeMatcher> hours       = new ArrayList<>();
        private final List<ScheduledTimeMatcher> daysOfMonth = new ArrayList<>();
        private final List<ScheduledTimeMatcher> daysOfWeek  = new ArrayList<>();
        private final List<ScheduledTimeMatcher> months      = new ArrayList<>();
        private final List<ScheduledTimeMatcher> years       = new ArrayList<>();
        private final RangerValidityRecurrence   recurrence;
        private       int                        intervalInMinutes = 0;


        public RangerRecurrenceEvaluator(RangerValidityRecurrence recurrence) {
            this.recurrence = recurrence;

            if (recurrence != null) {
                intervalInMinutes = RangerValidityRecurrence.ValidityInterval.getValidityIntervalInMinutes(recurrence.getInterval());

                if (intervalInMinutes > 0) {
                    addScheduledTime(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.minute, minutes);
                    addScheduledTime(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.hour, hours);
                    addScheduledTime(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfMonth, daysOfMonth);
                    addScheduledTime(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfWeek, daysOfWeek);
                    addScheduledTime(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.month, months);
                    addScheduledTime(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.year, years);
                }
            }
        }

        public boolean isApplicable(Calendar now) {
            boolean ret = false;

            RangerPerfTracer perf = null;

            if (RangerPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = RangerPerfTracer.getPerfTracer(PERF_LOG, "RangerRecurrenceEvaluator.isApplicable(accessTime=" + now.getTime().getTime() + ")");
            }

            if (recurrence != null && intervalInMinutes > 0) { // recurring schedule

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Access-Time:[" + now.getTime() + "]");
                }

                Calendar startOfInterval = getClosestPastEpoch(now);

                if (startOfInterval != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Start-of-Interval:[" + startOfInterval.getTime() + "]");
                    }

                    Calendar endOfInterval = (Calendar) startOfInterval.clone();
                    endOfInterval.add(Calendar.MINUTE, recurrence.getInterval().getMinutes());
                    endOfInterval.add(Calendar.HOUR, recurrence.getInterval().getHours());
                    endOfInterval.add(Calendar.DAY_OF_MONTH, recurrence.getInterval().getDays());

                    endOfInterval.getTime();    // for recomputation
                    now.getTime();

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("End-of-Interval:[" + endOfInterval.getTime() + "]");
                    }

                    ret = startOfInterval.compareTo(now) <= 0 && endOfInterval.compareTo(now) >= 0;
                }

            } else {
                ret = true;
            }

            RangerPerfTracer.log(perf);
            return ret;
        }

        private void addScheduledTime(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec fieldSpec, List<ScheduledTimeMatcher> list) {
            final String  str     = recurrence.getSchedule().getFieldValue(fieldSpec);
            final boolean isMonth = fieldSpec == RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.month;

            if (StringUtils.isNotBlank(str)) {
                String[] specs = str.split(",");

                for (String spec : specs) {
                    String[] range = spec.split("-");

                    if (range.length == 1) {
                        if (StringUtils.equals(range[0], RangerValidityRecurrence.RecurrenceSchedule.WILDCARD)) {
                            list.clear();
                            list.add(new ScheduledTimeAlwaysMatcher());

                            break;
                        } else {
                            list.add(new ScheduledTimeExactMatcher(Integer.valueOf(range[0]) - (isMonth ? 1 : 0)));
                        }
                    } else {
                        if (StringUtils.equals(range[0], RangerValidityRecurrence.RecurrenceSchedule.WILDCARD) || StringUtils.equals(range[1], RangerValidityRecurrence.RecurrenceSchedule.WILDCARD)) {
                            list.clear();
                            list.add(new ScheduledTimeAlwaysMatcher());

                            break;
                        } else {
                            list.add(new ScheduledTimeRangeMatcher(Integer.valueOf(range[0]) - (isMonth ? 1 : 0), Integer.valueOf(range[1]) - (isMonth ? 1 : 0)));
                        }
                    }
                }

                Collections.reverse(list);
            }

        }

    /*
    Given a Calendar object, get the closest, earlier Calendar object based on configured validity schedules.
    Returns - a valid Calendar object. Throws exception if any errors during processing or no suitable Calendar object is found.
    Description - Typically, a caller will call this with Calendar constructed with current time, and use returned object
                along with specified interval to ensure that next schedule time is after the input Calendar.
    Algorithm -   This involves doing a Calendar arithmetic (subtraction) with borrow. The tricky parts are ensuring that
                  Calendar arithmetic yields a valid Calendar object.
                    - Start with minutes, and then hours.
                    - Must make sure that the later of the two Calendars - one computed with dayOfMonth, another computed with
                      dayOfWeek - is picked
                    - For dayOfMonth calculation, consider that months have different number of days
    */

        private static class ValueWithBorrow {
            int value;
            boolean borrow;

            ValueWithBorrow() {
            }

            ValueWithBorrow(int value) {
                this(value, false);
            }

            ValueWithBorrow(int value, boolean borrow) {
                this.value = value;
                this.borrow = borrow;
            }

            void setValue(int value) {
                this.value = value;
            }

            void setBorrow(boolean borrow) {
                this.borrow = borrow;
            }

            int getValue() {
                return value;
            }

            boolean getBorrow() {
                return borrow;
            }

            @Override
            public String toString() {
                return "value=" + value + ", borrow=" + borrow;
            }
        }

        private Calendar getClosestPastEpoch(Calendar current) {
            Calendar ret = null;

            try {
                ValueWithBorrow input = new ValueWithBorrow();

                input.setValue(current.get(Calendar.MINUTE));
                input.setBorrow(false);
                ValueWithBorrow closestMinute = getPastFieldValueWithBorrow(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.minute, minutes, input);

                input.setValue(current.get(Calendar.HOUR_OF_DAY));
                input.setBorrow(closestMinute.borrow);
                ValueWithBorrow closestHour = getPastFieldValueWithBorrow(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.hour, hours, input);

                Calendar dayOfMonthCalendar = getClosestDayOfMonth(current, closestMinute, closestHour);

                Calendar dayOfWeekCalendar = getClosestDayOfWeek(current, closestMinute, closestHour);

                ret = getEarlierCalendar(dayOfMonthCalendar, dayOfWeekCalendar);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("ClosestPastEpoch:[" + (ret != null ? ret.getTime() : null) + "]");
                }

            } catch (Exception e) {
                LOG.error("Could not find ClosestPastEpoch, Exception=", e);
            }
            return ret;
        }

        private Calendar getClosestDayOfMonth(Calendar current, ValueWithBorrow closestMinute, ValueWithBorrow closestHour) throws Exception {
            Calendar ret = null;
            if (StringUtils.isNotBlank(recurrence.getSchedule().getDayOfMonth())) {
                int initialDayOfMonth = current.get(Calendar.DAY_OF_MONTH);

                int currentDayOfMonth = initialDayOfMonth, currentMonth = current.get(Calendar.MONTH), currentYear = current.get(Calendar.YEAR);
                int maximumDaysInPreviousMonth = getMaximumValForPreviousMonth(current);

                if (closestHour.borrow) {
                    initialDayOfMonth--;
                    Calendar dayOfMonthCalc = (GregorianCalendar) current.clone();
                    dayOfMonthCalc.add(Calendar.DAY_OF_MONTH, -1);
                    dayOfMonthCalc.getTime();
                    int previousDayOfMonth = dayOfMonthCalc.get(Calendar.DAY_OF_MONTH);
                    if (initialDayOfMonth < previousDayOfMonth) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Need to borrow from previous month, initialDayOfMonth:[" + initialDayOfMonth + "], previousDayOfMonth:[" + previousDayOfMonth + "], dayOfMonthCalc:[" + dayOfMonthCalc.getTime() + "]");
                        }
                        currentDayOfMonth = previousDayOfMonth;
                        currentMonth = dayOfMonthCalc.get(Calendar.MONTH);
                        currentYear = dayOfMonthCalc.get(Calendar.YEAR);
                        maximumDaysInPreviousMonth = getMaximumValForPreviousMonth(dayOfMonthCalc);
                    } else if (initialDayOfMonth == previousDayOfMonth) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("No need to borrow from previous month, initialDayOfMonth:[" + initialDayOfMonth + "], previousDayOfMonth:[" + previousDayOfMonth + "]");
                        }
                    } else {
                        LOG.error("Should not get here, initialDayOfMonth:[" + initialDayOfMonth + "], previousDayOfMonth:[" + previousDayOfMonth + "]");
                        throw new Exception("Should not get here, initialDayOfMonth:[" + initialDayOfMonth + "], previousDayOfMonth:[" + previousDayOfMonth + "]");
                    }
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("currentDayOfMonth:[" + currentDayOfMonth + "], maximumDaysInPreviourMonth:[" + maximumDaysInPreviousMonth + "]");
                }
                ValueWithBorrow input = new ValueWithBorrow();
                input.setValue(currentDayOfMonth);
                input.setBorrow(false);
                ValueWithBorrow closestDayOfMonth = null;
                do {
                    int i = 0;
                    try {
                        closestDayOfMonth = getPastFieldValueWithBorrow(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfMonth, daysOfMonth, input, maximumDaysInPreviousMonth);
                    } catch (Exception e) {
                        i++;
                        Calendar c = (GregorianCalendar) current.clone();
                        c.set(Calendar.YEAR, currentYear);
                        c.set(Calendar.MONTH, currentMonth);
                        c.set(Calendar.DAY_OF_MONTH, currentDayOfMonth);
                        c.add(Calendar.MONTH, -i);
                        c.getTime();
                        currentMonth = c.get(Calendar.MONTH);
                        currentYear = c.get(Calendar.YEAR);
                        currentDayOfMonth = c.get(Calendar.DAY_OF_MONTH);
                        maximumDaysInPreviousMonth = getMaximumValForPreviousMonth(c);
                        input.setValue(currentDayOfMonth);
                        input.setBorrow(false);
                    }
                } while (closestDayOfMonth == null);

                // Build calendar for dayOfMonth
                ret = new GregorianCalendar();
                ret.set(Calendar.DAY_OF_MONTH, closestDayOfMonth.value);
                ret.set(Calendar.HOUR_OF_DAY, closestHour.value);
                ret.set(Calendar.MINUTE, closestMinute.value);
                ret.set(Calendar.SECOND, 0);
                ret.set(Calendar.MILLISECOND, 0);

                ret.set(Calendar.YEAR, currentYear);

                if (closestDayOfMonth.borrow) {
                    ret.set(Calendar.MONTH, currentMonth - 1);
                } else {
                    ret.set(Calendar.MONTH, currentMonth);
                }
                ret.getTime(); // For recomputation

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Best guess using DAY_OF_MONTH:[" + ret.getTime() + "]");
                }
            }
            return ret;
        }

        private Calendar getClosestDayOfWeek(Calendar current, ValueWithBorrow closestMinute, ValueWithBorrow closestHour) throws Exception {
            Calendar ret = null;
            if (StringUtils.isNotBlank(recurrence.getSchedule().getDayOfWeek())) {
                ValueWithBorrow input = new ValueWithBorrow();

                input.setValue(current.get(Calendar.DAY_OF_WEEK));
                input.setBorrow(closestHour.borrow);


                ValueWithBorrow closestDayOfWeek = getPastFieldValueWithBorrow(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfWeek, daysOfWeek, input);

                int daysToGoback = closestHour.borrow ? 1 : 0;
                int range = RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfWeek.maximum - RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.dayOfWeek.minimum + 1;

                if (closestDayOfWeek.borrow) {
                    if (input.value - closestDayOfWeek.value != daysToGoback) {
                        daysToGoback = range + input.value - closestDayOfWeek.value;
                    }
                } else {
                    daysToGoback = input.value - closestDayOfWeek.value;
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Need to go back [" + daysToGoback + "] days to match dayOfWeek");
                }

                ret = (GregorianCalendar) current.clone();
                ret.set(Calendar.MINUTE, closestMinute.value);
                ret.set(Calendar.HOUR_OF_DAY, closestHour.value);
                ret.add(Calendar.DAY_OF_MONTH, (0 - daysToGoback));
	            ret.set(Calendar.SECOND, 0);
	            ret.set(Calendar.MILLISECOND, 0);

                ret.getTime();

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Best guess using DAY_OF_WEEK:[" + ret.getTime() + "]");
                }
            }
            return ret;

        }

        private int getMaximumValForPreviousMonth(Calendar current) {
            Calendar cal = (Calendar) current.clone();
            cal.add(Calendar.MONTH, -1);
            cal.getTime(); // For recomputation

            return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        }

        private Calendar getEarlierCalendar(Calendar dayOfMonthCalendar, Calendar dayOfWeekCalendar) throws Exception {

            Calendar withDayOfMonth = fillOutCalendar(dayOfMonthCalendar);
            if (LOG.isDebugEnabled()) {
                LOG.debug("dayOfMonthCalendar:[" + (withDayOfMonth != null ? withDayOfMonth.getTime() : null) + "]");
            }

            Calendar withDayOfWeek = fillOutCalendar(dayOfWeekCalendar);
            if (LOG.isDebugEnabled()) {
                LOG.debug("dayOfWeekCalendar:[" + (withDayOfWeek != null ? withDayOfWeek.getTime() : null) + "]");
            }

            if (withDayOfMonth != null && withDayOfWeek != null) {
                return withDayOfMonth.after(withDayOfWeek) ? withDayOfMonth : withDayOfWeek;
            } else if (withDayOfMonth == null) {
                return withDayOfWeek;
            } else {
                return withDayOfMonth;
            }
        }

        private Calendar fillOutCalendar(Calendar calendar) throws Exception {
            Calendar ret = null;

            if (calendar != null) {
                ValueWithBorrow input = new ValueWithBorrow(calendar.get(Calendar.MONTH));
                ValueWithBorrow closestMonth = getPastFieldValueWithBorrow(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.month, months, input);

                input.setValue(calendar.get(Calendar.YEAR));
                input.setBorrow(closestMonth.borrow);
                ValueWithBorrow closestYear = getPastFieldValueWithBorrow(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec.year, years, input);

                // Build calendar
                ret = (Calendar) calendar.clone();
                ret.set(Calendar.YEAR, closestYear.value);
                ret.set(Calendar.MONTH, closestMonth.value);
                ret.set(Calendar.SECOND, 0);

                ret.getTime(); // for recomputation
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Filled-out-Calendar:[" + ret.getTime() + "]");
                }
            }
            return ret;
        }

        private ValueWithBorrow getPastFieldValueWithBorrow(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec fieldSpec, List<ScheduledTimeMatcher> searchList, ValueWithBorrow input) throws Exception {
            return getPastFieldValueWithBorrow(fieldSpec, searchList, input, fieldSpec.maximum);
        }

        private ValueWithBorrow getPastFieldValueWithBorrow(RangerValidityRecurrence.RecurrenceSchedule.ScheduleFieldSpec fieldSpec, List<ScheduledTimeMatcher> searchList, ValueWithBorrow input, int maximum) throws Exception {

            ValueWithBorrow ret;
            boolean borrow = false;

            int value = input.value - (input.borrow ? 1 : 0);

            if (CollectionUtils.isNotEmpty(searchList)) {
                int range = fieldSpec.maximum - fieldSpec.minimum + 1;

                for (int i = 0; i < range; i++, value--) {
                    if (value < fieldSpec.minimum) {
                        value = maximum;
                        borrow = true;
                    }
                    for (ScheduledTimeMatcher time : searchList) {
                        if (time.isMatch(value)) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Found match in field:[" + fieldSpec + "], value:[" + value + "], borrow:[" + borrow + "], maximum:[" + maximum + "]");
                            }
                            return new ValueWithBorrow(value, borrow);
                        }
                    }
                }
                // Not found
                throw new Exception("No match found in field:[" + fieldSpec + "] for [input=" + input + "]");
            } else {
                if (value < fieldSpec.minimum) {
                    value = maximum;
                }
                ret = new ValueWithBorrow(value, false);
            }
            return ret;
        }
    }
}

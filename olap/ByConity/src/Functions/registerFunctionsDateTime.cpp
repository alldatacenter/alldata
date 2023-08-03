/*
 * Copyright 2016-2023 ClickHouse, Inc.
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


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

namespace DB
{

class FunctionFactory;

void registerFunctionToYear(FunctionFactory &);
void registerFunctionToQuarter(FunctionFactory &);
void registerFunctionToMonth(FunctionFactory &);
void registerFunctionToDayOfMonth(FunctionFactory &);
void registerFunctionToDayOfWeek(FunctionFactory &);
void registerFunctionToDayOfYear(FunctionFactory &);
void registerFunctionToHour(FunctionFactory &);
void registerFunctionToMinute(FunctionFactory &);
void registerFunctionToStartOfSecond(FunctionFactory &);
void registerFunctionToSecond(FunctionFactory &);
void registerFunctionToStartOfDay(FunctionFactory &);
void registerFunctionToMonday(FunctionFactory &);
void registerFunctionToISOWeek(FunctionFactory &);
void registerFunctionToISOYear(FunctionFactory &);
void registerFunctionToCustomWeek(FunctionFactory &);
void registerFunctionToModifiedJulianDay(FunctionFactory &);
void registerFunctionToStartOfMonth(FunctionFactory &);
void registerFunctionToStartOfQuarter(FunctionFactory &);
void registerFunctionToStartOfYear(FunctionFactory &);
void registerFunctionToStartOfMinute(FunctionFactory &);
void registerFunctionToStartOfFiveMinute(FunctionFactory &);
void registerFunctionToStartOfTenMinutes(FunctionFactory &);
void registerFunctionToStartOfFifteenMinutes(FunctionFactory &);
void registerFunctionToStartOfHour(FunctionFactory &);
void registerFunctionToStartOfInterval(FunctionFactory &);
void registerFunctionToStartOfISOYear(FunctionFactory &);
void registerFunctionToRelativeYearNum(FunctionFactory &);
void registerFunctionToRelativeQuarterNum(FunctionFactory &);
void registerFunctionToRelativeMonthNum(FunctionFactory &);
void registerFunctionToRelativeWeekNum(FunctionFactory &);
void registerFunctionToRelativeDayNum(FunctionFactory &);
void registerFunctionToRelativeHourNum(FunctionFactory &);
void registerFunctionToRelativeMinuteNum(FunctionFactory &);
void registerFunctionToRelativeSecondNum(FunctionFactory &);
void registerFunctionToTime(FunctionFactory &);
void registerFunctionNow(FunctionFactory &);
void registerFunctionNow64(FunctionFactory &);
void registerFunctionCurrentTime(FunctionFactory &);
void registerFunctionToday(FunctionFactory &);
void registerFunctionYesterday(FunctionFactory &);
void registerFunctionTimeSlot(FunctionFactory &);
void registerFunctionTimeSlots(FunctionFactory &);
void registerFunctionToYYYYMM(FunctionFactory &);
void registerFunctionToYYYYMMDD(FunctionFactory &);
void registerFunctionToYYYYMMDDhhmmss(FunctionFactory &);
void registerFunctionAddSeconds(FunctionFactory &);
void registerFunctionAddMinutes(FunctionFactory &);
void registerFunctionAddHours(FunctionFactory &);
void registerFunctionAddDays(FunctionFactory &);
void registerFunctionAddWeeks(FunctionFactory &);
void registerFunctionAddMonths(FunctionFactory &);
void registerFunctionAddQuarters(FunctionFactory &);
void registerFunctionAddYears(FunctionFactory &);
void registerFunctionSubtractSeconds(FunctionFactory &);
void registerFunctionSubtractMinutes(FunctionFactory &);
void registerFunctionSubtractHours(FunctionFactory &);
void registerFunctionSubtractDays(FunctionFactory &);
void registerFunctionSubtractWeeks(FunctionFactory &);
void registerFunctionSubtractMonths(FunctionFactory &);
void registerFunctionSubtractQuarters(FunctionFactory &);
void registerFunctionSubtractYears(FunctionFactory &);
void registerFunctionDateDiff(FunctionFactory &);
void registerFunctionDateName(FunctionFactory &);
void registerFunctionToTimeZone(FunctionFactory &);
void registerFunctionFormatDateTime(FunctionFactory &);
void registerFunctionFromModifiedJulianDay(FunctionFactory &);
void registerFunctionDateTrunc(FunctionFactory &);

void registerFunctiontimezoneOffset(FunctionFactory &);
void registerFunctionNextDay(FunctionFactory &);
void registerFunctionLastDay(FunctionFactory &);
void registerFunctionDateFormat(FunctionFactory & factory);

void registerFunctionsDateTime(FunctionFactory & factory)
{
    registerFunctionToYear(factory);
    registerFunctionToQuarter(factory);
    registerFunctionToMonth(factory);
    registerFunctionToDayOfMonth(factory);
    registerFunctionToDayOfWeek(factory);
    registerFunctionToDayOfYear(factory);
    registerFunctionToHour(factory);
    registerFunctionToMinute(factory);
    registerFunctionToSecond(factory);
    registerFunctionToStartOfDay(factory);
    registerFunctionToMonday(factory);
    registerFunctionToISOWeek(factory);
    registerFunctionToISOYear(factory);
    registerFunctionToCustomWeek(factory);
    registerFunctionToModifiedJulianDay(factory);
    registerFunctionToStartOfMonth(factory);
    registerFunctionToStartOfQuarter(factory);
    registerFunctionToStartOfYear(factory);
    registerFunctionToStartOfSecond(factory);
    registerFunctionToStartOfMinute(factory);
    registerFunctionToStartOfFiveMinute(factory);
    registerFunctionToStartOfTenMinutes(factory);
    registerFunctionToStartOfFifteenMinutes(factory);
    registerFunctionToStartOfHour(factory);
    registerFunctionToStartOfInterval(factory);
    registerFunctionToStartOfISOYear(factory);
    registerFunctionToRelativeYearNum(factory);
    registerFunctionToRelativeQuarterNum(factory);
    registerFunctionToRelativeMonthNum(factory);
    registerFunctionToRelativeWeekNum(factory);
    registerFunctionToRelativeDayNum(factory);
    registerFunctionToRelativeHourNum(factory);
    registerFunctionToRelativeMinuteNum(factory);
    registerFunctionToRelativeSecondNum(factory);
    registerFunctionToTime(factory);
    registerFunctionNow(factory);
    registerFunctionNow64(factory);
    registerFunctionCurrentTime(factory);
    registerFunctionToday(factory);
    registerFunctionYesterday(factory);
    registerFunctionTimeSlot(factory);
    registerFunctionTimeSlots(factory);
    registerFunctionToYYYYMM(factory);
    registerFunctionToYYYYMMDD(factory);
    registerFunctionToYYYYMMDDhhmmss(factory);
    registerFunctionAddSeconds(factory);
    registerFunctionAddMinutes(factory);
    registerFunctionAddHours(factory);
    registerFunctionAddDays(factory);
    registerFunctionAddWeeks(factory);
    registerFunctionAddMonths(factory);
    registerFunctionAddQuarters(factory);
    registerFunctionAddYears(factory);
    registerFunctionSubtractSeconds(factory);
    registerFunctionSubtractMinutes(factory);
    registerFunctionSubtractHours(factory);
    registerFunctionSubtractDays(factory);
    registerFunctionSubtractWeeks(factory);
    registerFunctionSubtractMonths(factory);
    registerFunctionSubtractQuarters(factory);
    registerFunctionSubtractYears(factory);
    registerFunctionDateDiff(factory);
    registerFunctionDateName(factory);
    registerFunctionToTimeZone(factory);
    registerFunctionFormatDateTime(factory);
    registerFunctionFromModifiedJulianDay(factory);
    registerFunctionDateTrunc(factory);
    registerFunctiontimezoneOffset(factory);
    registerFunctionNextDay(factory);
    registerFunctionLastDay(factory);
    registerFunctionDateFormat(factory);
}

}

// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import { Task, ViewMode } from '../types/public-types';
import moment from 'moment';
import { min, max, flatten, compact } from 'lodash';

type DateTimeFormatOptions = Intl.DateTimeFormatOptions;
type DateTimeFormat = Intl.DateTimeFormat;

type DateHelperScales = 'year' | 'month' | 'day' | 'hour' | 'minute' | 'second' | 'millisecond';

const intlDTCache = {};
export const getCachedDateTimeFormat = (
  locString: string | string[],
  opts: DateTimeFormatOptions = {},
): DateTimeFormat => {
  const key = JSON.stringify([locString, opts]);
  let dtf = intlDTCache[key];
  if (!dtf) {
    dtf = new Intl.DateTimeFormat(locString, opts);
    intlDTCache[key] = dtf;
  }
  return dtf;
};

export const addToDate = (date: Date, quantity: number, scale: DateHelperScales) => {
  const newDate = new Date(
    date.getFullYear() + (scale === 'year' ? quantity : 0),
    date.getMonth() + (scale === 'month' ? quantity : 0),
    date.getDate() + (scale === 'day' ? quantity : 0),
    date.getHours() + (scale === 'hour' ? quantity : 0),
    date.getMinutes() + (scale === 'minute' ? quantity : 0),
    date.getSeconds() + (scale === 'second' ? quantity : 0),
    date.getMilliseconds() + (scale === 'millisecond' ? quantity : 0),
  );
  return newDate;
};

export const startOfDate = (date: Date, scale: DateHelperScales) => {
  const scores = ['millisecond', 'second', 'minute', 'hour', 'day', 'month', 'year'];

  const shouldReset = (_scale: DateHelperScales) => {
    const maxScore = scores.indexOf(scale);
    return scores.indexOf(_scale) <= maxScore;
  };
  const newDate = new Date(
    date.getFullYear(),
    shouldReset('year') ? 0 : date.getMonth(),
    shouldReset('month') ? 1 : date.getDate(),
    shouldReset('day') ? 0 : date.getHours(),
    shouldReset('hour') ? 0 : date.getMinutes(),
    shouldReset('minute') ? 0 : date.getSeconds(),
    shouldReset('second') ? 0 : date.getMilliseconds(),
  );
  return newDate;
};

export const calcDateDurationByHNumber = (h_number: number, oldStartDate: Date, oldEndDate: Date) => {
  const today = moment(new Date()).startOf('day');
  let startDate: Date = oldStartDate;
  let endDate: Date = oldEndDate;

  // if (today - h_number / 2) is less than startDate, startDate = today - h_number / 2
  if (
    moment(today)
      .subtract(Math.floor(h_number / 2), 'days')
      .isBefore(moment(startDate), 'days')
  ) {
    startDate = new Date(
      moment(today)
        .subtract(Math.floor(h_number / 2), 'days')
        .valueOf(),
    );
  }
  // if (today + h_number / 2) is more than endDate, endDate = today + h_number / 2
  if (
    moment(today)
      .add(Math.floor(h_number / 2), 'days')
      .isAfter(moment(endDate), 'days')
  ) {
    endDate = new Date(
      moment(today)
        .add(Math.floor(h_number / 2), 'days')
        .valueOf(),
    );
  }
  return [startDate, endDate];
};

export const ganttDateRange = (tasks: Task[], viewMode: ViewMode) => {
  let newStartDate: Date = tasks[0].start || 0;
  let newEndDate: Date = tasks[0].start || 0;

  const timeArr = compact(flatten(tasks.map((item) => [item.start, item.end])));

  const minTime = min(timeArr);
  const maxTime = max(timeArr);
  newStartDate = minTime && new Date(minTime);
  newEndDate = maxTime && new Date(maxTime);

  if (!newStartDate) {
    newStartDate = new Date(moment().subtract(15, 'days'));
  }
  if (!newEndDate || newEndDate.getTime() === newStartDate.getTime()) {
    newEndDate = new Date(moment(newStartDate).subtract(-30, 'days'));
  }

  // start time is bigger then end time
  if (newStartDate.getTime() > newEndDate.getTime()) {
    [newStartDate, newEndDate] = [newEndDate, newStartDate];
  }

  switch (viewMode) {
    case ViewMode.Month:
      newStartDate = addToDate(newStartDate, -1, 'month');
      newStartDate = startOfDate(newStartDate, 'month');
      newEndDate = addToDate(newEndDate, 1, 'year');
      newEndDate = startOfDate(newEndDate, 'year');
      break;
    case ViewMode.Week:
      newStartDate = startOfDate(newStartDate, 'day');
      newEndDate = startOfDate(newEndDate, 'day');
      newStartDate = addToDate(getMonday(newStartDate), -7, 'day');
      newEndDate = addToDate(newEndDate, 1.5, 'month');
      break;
    case ViewMode.Day:
      newStartDate = startOfDate(newStartDate, 'day');
      newEndDate = startOfDate(newEndDate, 'day');
      newStartDate = addToDate(newStartDate, -1, 'day');
      newEndDate = addToDate(newEndDate, 19, 'day');
      break;
    case ViewMode.QuarterDay:
      newStartDate = startOfDate(newStartDate, 'day');
      newEndDate = startOfDate(newEndDate, 'day');
      newStartDate = addToDate(newStartDate, -1, 'day');
      newEndDate = addToDate(newEndDate, 66, 'hour'); // 24(1 day)*3 - 6
      break;
    case ViewMode.HalfDay:
      newStartDate = startOfDate(newStartDate, 'day');
      newEndDate = startOfDate(newEndDate, 'day');
      newStartDate = addToDate(newStartDate, -1, 'day');
      newEndDate = addToDate(newEndDate, 108, 'hour'); // 24(1 day)*5 - 12
      break;
    default:
      break;
  }
  return [newStartDate, newEndDate];
};

export const seedDates = (startDate: Date, endDate: Date, viewMode: ViewMode) => {
  let currentDate: Date = new Date(startDate);
  const dates: Date[] = [currentDate];
  while (currentDate < endDate) {
    switch (viewMode) {
      case ViewMode.Month:
        currentDate = addToDate(currentDate, 1, 'month');
        break;
      case ViewMode.Week:
        currentDate = addToDate(currentDate, 7, 'day');
        break;
      case ViewMode.Day:
        currentDate = addToDate(currentDate, 1, 'day');
        break;
      case ViewMode.HalfDay:
        currentDate = addToDate(currentDate, 12, 'hour');
        break;
      case ViewMode.QuarterDay:
        currentDate = addToDate(currentDate, 6, 'hour');
        break;
      default:
        break;
    }
    dates.push(currentDate);
  }
  return dates;
};

export const getLocaleMonth = (date: Date, locale: string) => {
  let bottomValue = getCachedDateTimeFormat(locale, {
    month: 'long',
  }).format(date);
  bottomValue = bottomValue.replace(bottomValue[0], bottomValue[0].toLocaleUpperCase());
  return bottomValue;
};

/**
 * Returns monday of current week
 * @param date date for modify
 */
const getMonday = (date: Date) => {
  const day = date.getDay();
  const diff = date.getDate() - day + (day === 0 ? -6 : 1); // adjust when day is sunday
  return new Date(date.setDate(diff));
};

export const getWeekNumberISO8601 = (date: Date) => {
  const tmpDate = new Date(date.valueOf());
  const dayNumber = (tmpDate.getDay() + 6) % 7;
  tmpDate.setDate(tmpDate.getDate() - dayNumber + 3);
  const firstThursday = tmpDate.valueOf();
  tmpDate.setMonth(0, 1);
  if (tmpDate.getDay() !== 4) {
    tmpDate.setMonth(0, 1 + ((4 - tmpDate.getDay() + 7) % 7));
  }
  const weekNumber = (1 + Math.ceil((firstThursday - tmpDate.valueOf()) / 604800000)).toString();

  if (weekNumber.length === 1) {
    return `0${weekNumber}`;
  } else {
    return weekNumber;
  }
};

export const getDaysInMonth = (month: number, year: number) => {
  return new Date(year, month + 1, 0).getDate();
};

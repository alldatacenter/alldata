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
import {
  transformRange,
  translateAutoRefreshDuration,
  defaultFormat,
  translateRelativeTime,
} from 'common/components/time-select/common';
import moment from 'moment';

describe('time-select-common', () => {
  it('transformRange should work well', () => {
    const start = moment();
    const end = moment();
    const quickRes = transformRange({ mode: 'quick', quick: 'days:1', customize: {} });
    expect(quickRes.dateStr).toBe('last 1 days');
    expect(quickRes.date).toHaveLength(2);
    expect(quickRes.date[0].isSame(start.subtract(1, 'days'), 'date')).toBeTruthy();
    expect(quickRes.date[1].isSame(end, 'date')).toBeTruthy();
    const customizeRes = transformRange({
      mode: 'customize',
      quick: undefined,
      customize: {
        start,
        end,
      },
    });
    expect(customizeRes.dateStr).toBe(`${start.format(defaultFormat)} - ${end.format(defaultFormat)}`);
    expect(customizeRes.date).toHaveLength(2);
    expect(customizeRes.date[0].isSame(start, 'date')).toBeTruthy();
    expect(customizeRes.date[1].isSame(end, 'date')).toBeTruthy();
  });
  it('translateAutoRefreshDuration should work well', () => {
    expect(translateAutoRefreshDuration(1, 'seconds')).toBe(1000);
    expect(translateAutoRefreshDuration(1, 'minutes')).toBe(60000);
    expect(translateAutoRefreshDuration(1, 'hours')).toBe(3600000);
    expect(translateAutoRefreshDuration(1, 'days')).toBe(86400000);
  });
  it('translateRelativeTime should work well', () => {
    const start = moment();
    const end = moment();
    const minutes = translateRelativeTime('minutes', 10);
    expect(minutes[0].isSame(start.clone().subtract(10, 'minutes'))).toBeTruthy();
    const today = translateRelativeTime('today');
    expect(today[0].isSame(start.clone().startOf('day'))).toBeTruthy();
    const yesterday = translateRelativeTime('yesterday');
    expect(yesterday[0].isSame(start.clone().startOf('day').subtract(1, 'days'))).toBeTruthy();
    expect(yesterday[1].isSame(end.clone().endOf('day').subtract(1, 'days'))).toBeTruthy();
    const currentWeek = translateRelativeTime('currentWeek');
    expect(currentWeek[0].isSame(start.clone().startOf('week'))).toBeTruthy();
    const lastMonth = translateRelativeTime('lastMonth');
    expect(lastMonth[0].isSame(start.clone().startOf('month').subtract(1, 'months'))).toBeTruthy();
    expect(lastMonth[1].isSame(end.clone().subtract(1, 'months').endOf('month'))).toBeTruthy();
    const lastWeek = translateRelativeTime('lastWeek');
    expect(lastWeek[0].isSame(start.clone().startOf('week').subtract(1, 'weeks'))).toBeTruthy();
    expect(lastWeek[1].isSame(end.clone().endOf('week').subtract(1, 'weeks'))).toBeTruthy();
    const currentMonth = translateRelativeTime('currentMonth');
    expect(currentMonth[0].isSame(start.clone().startOf('month'))).toBeTruthy();
    const defaultDate = translateRelativeTime('day');
    expect(defaultDate[0].isSame(start.clone().startOf('day'))).toBeTruthy();
  });
});

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

import React from 'react';
import { shallow } from 'enzyme';
import {
  camel2DashName,
  getStrRealLen,
  px2Int,
  getDateDuration,
  cutStr,
  secondsToTime,
  daysRange,
  fromNow,
  getTimeSpan,
  formatTime,
} from 'common/utils';
import moment from 'moment';

const title = 'erda cloud';

describe('str-num-date', () => {
  it('camel2DashName', () => {
    expect(camel2DashName('ErdaCloud')).toBe('erda-cloud');
  });
  it('getStrRealLen', () => {
    expect(getStrRealLen(title, true, 5)).toBe(4);
    expect(getStrRealLen(title, true, 15)).toBe(10);
    expect(getStrRealLen(title, false, 5)).toBe(10);
    expect(getStrRealLen(title, false, 15)).toBe(10);
    expect(getStrRealLen('尔达云', true, 3)).toBe(1);
    expect(getStrRealLen('尔达云', true, 12)).toBe(3);
    expect(getStrRealLen('尔达云', false, 3)).toBe(6);
    expect(getStrRealLen('尔达云', false, 12)).toBe(6);
  });
  it('px2Int', () => {
    expect(px2Int('12px')).toBe(12);
    expect(px2Int('12other')).toBe(12);
    expect(px2Int('12')).toBe(12);
    expect(px2Int('other')).toBeNaN();
  });
  it('getDateDuration', () => {
    expect(getDateDuration('2021-04-24', '2021-04-25')).toBe('a day');
    expect(getDateDuration('2021-04-24 12:12:12', '2021-04-24 12:12:20')).toBe('8 second(s)');
    expect(getDateDuration('2021-04-24 12:12:12', '2021-04-24 12:13:12')).toBe('a minute');
  });
  it('cutStr', () => {
    expect(cutStr(null)).toBe('');
    const wrapper = shallow(<div>{cutStr(title, 0, { suffix: '...', showTip: true })}</div>);
    expect(wrapper.find('Tooltip').prop('title')).toBe(title);
    expect(wrapper.find('Tooltip').children().text()).toBe('...');
  });
  it('secondsToTime', () => {
    expect(secondsToTime()).toBeUndefined();
    expect(secondsToTime(-1)).toBe(-1);
    expect(secondsToTime(1)).toBe('00:01');
    expect(secondsToTime(3750, false)).toBe('1:02:30');
    expect(secondsToTime(3750, true)).toBe('1hour2minutes30second(s)');
    expect(secondsToTime(20, true)).toBe('20second(s)');
    expect(secondsToTime(90, true)).toBe('1minutes30second(s)');
  });
  it('daysRange', () => {
    const testDaysRange = (num: number) => {
      const curDay = moment().startOf('day');
      expect(daysRange(1)).toStrictEqual({
        start: curDay.subtract(num - 1, 'days').valueOf(),
        end: curDay.add(num, 'days').valueOf(),
      });
    };
    testDaysRange(1);
  });
  it('fromNow', () => {
    const curr = moment();
    const wrapper = shallow(
      <div>
        {fromNow(curr)}
        {fromNow(curr, { prefix: title })}
      </div>,
    );
    expect(wrapper.find('Tooltip').at(0).prop('title')).toBe(curr.format('YYYY-MM-DD HH:mm:ss'));
    expect(wrapper.find('Tooltip').at(0).children().text()).toBe(curr.fromNow());
    expect(wrapper.find('Tooltip').at(1).prop('title')).toBe(`${title}${curr.format('YYYY-MM-DD HH:mm:ss')}`);
  });
  it('getTimeSpan', () => {
    const start = moment('2021-04-20 12:00:00');
    const startTimeMs = start.valueOf();
    const startTime = parseInt(`${startTimeMs / 1000}`, 10);
    const startTimeNs = startTimeMs * 1000000;
    const end = moment();
    const endTimeMs = end.valueOf();
    const endTime = parseInt(`${(endTimeMs as number) / 1000}`, 10);
    const endTimeNs = endTimeMs * 1000000;
    expect(getTimeSpan([start, end])).toStrictEqual({
      hours: 1,
      seconds: Math.ceil(endTime - startTime),
      endTime,
      startTime,
      endTimeMs,
      startTimeMs,
      endTimeNs,
      startTimeNs,
      time: { startTime, endTime },
      timeMs: { startTimeMs, endTimeMs },
      timeNs: { startTimeNs, endTimeNs },
    });
  });
  it('formatTime', () => {
    const dateStr = '2021-04-20 12:00:00';
    expect(formatTime(0)).toBeNull();
    expect(formatTime(dateStr)).toBe('2021-04-20');
    expect(formatTime(dateStr, 'YYYY-MM-DD HH:mm:ss')).toBe(dateStr);
  });
});

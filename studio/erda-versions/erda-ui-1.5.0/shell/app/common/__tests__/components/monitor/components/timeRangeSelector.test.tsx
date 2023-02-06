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
import TimeSelector from 'common/components/monitor/components/timeRangeSelector';
import { mount } from 'enzyme';
import moment from 'moment';
import { getTimeSpan } from 'common/utils';

const start = moment().add(-7, 'days');
const start1 = moment().add(-3, 'days');
const end = moment();
const timeSpan = getTimeSpan([start, end]);
const nextTimeSpan = getTimeSpan([start1, end]);

describe('TimeSelector', () => {
  it('should work well', () => {
    const disabledDateFn = jest.fn();
    const onChangeTimeFn = jest.fn();
    const wrapper = mount(
      <TimeSelector query={{ start, end }} timeSpan={timeSpan} inline onChangeTime={onChangeTimeFn} />,
    );
    expect(wrapper.find('.monitor-time-selector-inline')).toExist();
    const value = wrapper.find('RangePicker').at(0).prop('value') || [];
    expect(value[0].isSame(start, 'date')).toBeTruthy();
    expect(value[1].isSame(end, 'date')).toBeTruthy();
    wrapper.find('div.ant-picker-range').simulate('click');
    wrapper.find('RangePicker').at(0).prop('onChange')([start1, end]);
    const { value: newValue = [] } = wrapper.state();
    expect(newValue[0].isSame(start1, 'date')).toBeTruthy();
    wrapper.find('RangePicker').at(0).prop('onOpenChange')(false);
    const { value: newValue1 = [] } = wrapper.state();
    expect(newValue1[0].isSame(start, 'date')).toBeTruthy();
    expect(onChangeTimeFn).toHaveBeenCalled();
    wrapper.setProps({
      inline: false,
      timeSpan: nextTimeSpan,
      disabledDate: disabledDateFn,
    });
    expect(wrapper.find('.monitor-time-selector')).toExist();
    expect(disabledDateFn).toHaveBeenCalled();
  });
});

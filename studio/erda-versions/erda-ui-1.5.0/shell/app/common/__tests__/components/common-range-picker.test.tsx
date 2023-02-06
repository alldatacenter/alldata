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
import { CommonRangePicker } from 'common';
import { mount } from 'enzyme';
import { act } from 'react-dom/test-utils';
import moment from 'moment';

const yesterday = moment().add(-1, 'days');
const today = moment();
const tomorrow = moment().add(1, 'days');

describe('CommonRangePicker', () => {
  it('should render well', () => {
    const fn = jest.fn();
    const wrapper = mount(<CommonRangePicker onOk={fn} defaultTime={[today, tomorrow]} />);
    // expect(wrapper).toMatchSnapshot();
    act(() => {
      wrapper.find('RangePicker').at(0).prop('onChange')([yesterday, today]);
    });
    wrapper.update();
    const date = wrapper.find('RangePicker').at(0).prop('value');
    expect(date).toHaveLength(2);
    expect(date[0].isSame(yesterday, 'date')).toBeTruthy();
    expect(date[1].isSame(today, 'date')).toBeTruthy();
    expect(wrapper.find('RangePicker').at(0).prop('disabledDate')(yesterday)).toBeFalsy();
    expect(wrapper.find('RangePicker').at(0).prop('disabledDate')(tomorrow)).toBeTruthy();
    expect(fn).toHaveBeenCalled();
  });
});

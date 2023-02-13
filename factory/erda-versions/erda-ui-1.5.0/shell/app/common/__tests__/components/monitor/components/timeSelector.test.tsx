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
import TimeSelector from 'common/components/monitor/components/timeSelector';
import { shallow } from 'enzyme';
import moment from 'moment';
import { getTimeSpan } from 'common/utils';

const start = moment().add(-7, 'days');
const end = moment();
const timeSpan = getTimeSpan([start, end]);

describe('TimeSelector', () => {
  it('TimeSelector should work fine', () => {
    const onChangeTimeFn = jest.fn();
    const wrapper = shallow(<TimeSelector onChangeTime={onChangeTimeFn} timeSpan={timeSpan} inline />);
    expect(wrapper).toHaveClassName('monitor-time-selector-inline');
    wrapper.find('ForwardRef').prop('onChange')();
    expect(onChangeTimeFn).toHaveBeenCalled();
  });
  it('render with defaultTime', () => {
    const onChangeTimeFn = jest.fn();
    const wrapper = shallow(<TimeSelector onChangeTime={onChangeTimeFn} timeSpan={timeSpan} defaultTime={1} />);
    expect(wrapper).toHaveClassName('monitor-time-selector');
  });
});

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
import PureMonitorChart from 'common/components/monitor/monitor-chart';
import { getTimeSpan } from 'common/utils';
import { shallow } from 'enzyme';

describe('PureMonitorChart', () => {
  it('should render normally', () => {
    const timeSpan = getTimeSpan();
    const data = {
      data: [1],
    };
    const wrapper = shallow(<PureMonitorChart title="PureMonitorChart title" timeSpan={timeSpan} />);
    expect(wrapper.find('.monitor-chart-title').text()).toBe('PureMonitorChart title');
    expect(wrapper.find('MonitorChartNew').prop('timeSpan')).toStrictEqual(timeSpan);
    expect(wrapper.find('MonitorChartNew').prop('data')).toStrictEqual({});
    wrapper.setProps({
      data,
    });
    expect(wrapper.find('MonitorChartNew').prop('data')).toStrictEqual(data);
  });
});

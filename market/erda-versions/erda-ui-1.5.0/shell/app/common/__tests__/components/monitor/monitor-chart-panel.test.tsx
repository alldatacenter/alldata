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
import MonitorChartPanel from 'common/components/monitor/monitor-chart-panel';
import { shallow } from 'enzyme';
import { act } from 'react-dom/test-utils';

describe('MonitorChartPanel', () => {
  it('render without resourceType', () => {
    const wrapper = shallow(<MonitorChartPanel resourceId="A" defaultTime={1} />);
    expect(wrapper).toBeEmptyRender();
  });
  it('render without resourceId', () => {
    const wrapper = shallow(<MonitorChartPanel resourceType="A" />);
    expect(wrapper).toBeEmptyRender();
  });
  it('render with metrics length less than 4', () => {
    const metrics = {
      a: { name: 'a', parameters: { a: 1 } },
      b: { name: 'b' },
      c: { name: 'c' },
      d: { name: 'd' },
    };
    const wrapper = shallow(<MonitorChartPanel resourceType="a" resourceId="a" metrics={metrics} />);
    expect(wrapper.find('MonitorChart')).toHaveLength(4);
    expect(wrapper.find('IF').prop('check')).toBeFalsy();
  });
  it('render with metrics length greater than 4', () => {
    const metrics = {
      a: { name: 'a' },
      b: { name: 'b' },
      c: { name: 'c' },
      d: { name: 'd' },
      e: { name: 'e' },
      f: { name: 'f' },
      g: { name: 'g' },
      h: { name: 'h' },
      i: { name: 'i' },
    };
    const wrapper = shallow(<MonitorChartPanel resourceType="a" resourceId="a" metrics={metrics} />);
    expect(wrapper.find('MonitorChart')).toHaveLength(4);
    expect(wrapper.find('IF').prop('check')).toBeTruthy();
    act(() => {
      wrapper.find('.show-all').simulate('click');
      expect(wrapper.find('MonitorChart')).toHaveLength(8);
    });
  });
});

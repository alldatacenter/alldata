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
import TimeSelectorContainer from 'common/components/monitor/timeSelector';
import { mount } from 'enzyme';
import routeInfoStore from 'core/stores/route';

describe('TimeSelectorContainer', () => {
  beforeAll(() => {
    routeInfoStore.reducers.$_updateRouteInfo({
      pathname: process.env.mock_pathname,
      search: process.env.mock_search,
    });
  });
  it('render TimeSelector', () => {
    const wrapper = mount(<TimeSelectorContainer />);
    expect(wrapper.find('RangePicker')).toExist();
    wrapper.setProps({
      rangeMode: false,
    });
    expect(wrapper.find('RangePicker')).not.toExist();
  });
});

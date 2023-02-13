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
import { CircleProgress } from 'common';
import { mount } from 'enzyme';

describe('CircleProgress', () => {
  it('render', () => {
    const wrapper = mount(<CircleProgress left={0} right={0} autoFontSize={false} />);
    expect(wrapper.find('Progress.circle-progress').find('Progress.circle-progress').prop('percent')).toBe(0);
    expect(wrapper.find('Progress.circle-progress')).not.toHaveClassName('small-font');
    wrapper.setProps({
      right: 100,
      left: 90.95,
      autoFontSize: true,
    });
    expect(wrapper.find('Progress.circle-progress')).toHaveClassName('small-font');
  });
});

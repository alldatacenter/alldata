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
import { getLabel } from 'common/utils';

describe('getLabel', () => {
  const label = 'i am a label';
  const tips = 'i an a tips';
  it('should only label', () => {
    expect(getLabel(label)).toBe(label);
  });
  it('should not show tips', () => {
    const Comp = getLabel(label, tips, false);
    const wrapper = shallow(<div>{Comp}</div>);
    expect(wrapper.find('span').text()).toContain(label);
    expect(wrapper).not.toContain(label);
  });
  it('should not show tips', () => {
    const Comp = getLabel(label, tips, true);
    const wrapper = shallow(<div>{Comp}</div>);
    expect(wrapper.find('span').at(0).text()).toContain(label);
    expect(wrapper.find('span').at(1).text()).toContain('*');
    expect(wrapper.find('Tooltip').prop('title')).toBe(tips);
  });
});

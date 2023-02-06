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
import { EmptyHolder, EmptyListHolder, Holder } from 'common';
import { shallow } from 'enzyme';

describe('EmptyHolder Comp', () => {
  it('render EmptyHolder', () => {
    const wrapper = shallow(<EmptyHolder />);
    expect(wrapper.children().at(0).prop('type')).toBe('empty');
    expect(wrapper.children().at(1).text()).toContain('no data');
    wrapper.setProps({
      icon: 'icon',
      tip: 'this is a tip',
      relative: true,
    });
    expect(wrapper.children().at(0).prop('type')).toBe('icon');
    expect(wrapper.children().at(1).text()).toContain('this is a tip');
    expect(wrapper).toHaveClassName('relative');
  });
  it('render EmptyListHolder', () => {
    const wrapper = shallow(<EmptyListHolder />);
    expect(wrapper.children().at(0).prop('type')).toBe('empty-s');
    expect(wrapper.children().at(1).text()).toContain('no data');
    wrapper.setProps({
      icon: 'icon',
      tip: 'this is a tip',
      relative: true,
    });
    expect(wrapper.children().at(0).prop('type')).toBe('icon');
    expect(wrapper.children().at(1).text()).toContain('this is a tip');
  });
  it('render EmptyListHolder', () => {
    const wrapper = shallow(
      <Holder page={false} showHolder={() => false}>
        <div className="holder-children">children</div>
      </Holder>,
    );
    expect(wrapper.find('.holder-children')).toExist();
  });
});

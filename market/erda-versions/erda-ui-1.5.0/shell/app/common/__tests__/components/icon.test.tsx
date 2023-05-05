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
import { Icon } from 'common';
import { mount } from 'enzyme';

describe('icon', () => {
  const iconType = 'loading';
  it('should render base icon', () => {
    const onClick = jest.fn();
    const wrapper = mount(<Icon type={iconType} onClick={onClick} className="icon-class" />);
    wrapper.find('.iconfont').simulate('click');
    expect(onClick).toHaveBeenCalled();
    expect(wrapper.find(`i.icon-${iconType}`).length).toBe(1);
    expect(wrapper.find('i')).toHaveClassName('icon-class');
  });
  it('should render colur icon', () => {
    const onClick = jest.fn();
    const wrapper = mount(<Icon type={iconType} color onClick={onClick} className="icon-class" />);
    wrapper.find('.icon').simulate('click');
    expect(onClick).toHaveBeenCalled();
    expect(wrapper.find('svg.icon').length).toBe(1);
    expect(wrapper.find('use')).toHaveHTML(`<use xlink:href="#icon-${iconType}"></use>`);
    expect(wrapper.find('svg')).toHaveClassName('icon-class');
  });
  it('should type is ReactElement', () => {
    const type = <span>erda</span>;
    const wrapper = mount(<Icon type={type} className="icon-class" />);
    expect(wrapper.children()).toHaveHTML('<span>erda</span>');
  });
  it('should type is preset', () => {
    const wrapper = mount(<Icon type="ISSUE_ICON.issue.REQUIREMENT" />);
    expect(wrapper.find('.requirement')).toExist();
  });
});

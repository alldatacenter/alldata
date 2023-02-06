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
import { DropdownSelect } from 'common';
import { mount } from 'enzyme';

const menuList = [
  {
    name: 'DEV',
    key: 'dev',
  },
  {
    name: 'TEST',
    key: 'test',
  },
  {
    name: 'STAGING',
    key: 'staging',
  },
  {
    name: 'PROD',
    key: 'prod',
    children: [
      {
        name: 'PROD-1',
        key: 'prod-1',
      },
      {
        name: 'PROD-2',
        key: 'prod-2',
      },
    ],
  },
];

describe('DropdownSelect', () => {
  it('should render well', () => {
    const wrapper = mount(<DropdownSelect menuList={menuList} />);
    const Dropdown = wrapper.find('Dropdown').at(0);
    const overlay = Dropdown.prop('overlay').props.children;
    expect(React.Children.count(overlay)).toBe(menuList.length);
    const subMenu = React.Children.toArray(overlay)[menuList.length - 1];
    expect(React.Children.count(subMenu.props.children)).toBe(2);
  });
});

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
import { ConfigLayout } from 'common';
import { mount } from 'enzyme';

interface ISection {
  title?: string | React.ReactNode;
  titleExtra?: JSX.Element | null;
  titleOperate?: JSX.Element | null;
  children: JSX.Element;
  desc?: string | React.ReactNode;
  highlight?: string;
  titleProps?: object;
  descProps?: object;
}
const sectionList: ISection[] = [
  {
    title: 'sectionList1-title',
    titleExtra: <div>sectionList1-titleExtra</div>,
    titleOperate: <div>sectionList1-titleOperate</div>,
    desc: 'sectionList1-description',
    children: <div>sectionList1-children</div>,
    highlight: 'sectionList1-highlight',
    titleProps: { name: 'sectionList1-titleProps' },
    descProps: { name: 'sectionList1-descProps' },
  },
  {
    title: 'sectionList2-title',
    desc: 'sectionList2-description',
    children: <div>sectionList2-children</div>,
  },
];

describe('ConfigLayout', () => {
  it('should render successfully', () => {
    const wrapper = mount(<ConfigLayout sectionList={sectionList} />);
    expect(wrapper.find('ConfigSection')).toHaveLength(sectionList.length);
  });
});

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
import { SettingTabs } from 'common';
import { mount } from 'enzyme';
import * as utils from 'common/utils/query-string';
import routeInfoStore from 'core/stores/route';

const dataSource1 = [
  {
    groupTitle: 'common Menu',
    groupKey: 'common',
    tabGroup: [
      {
        tabTitle: 'application information',
        tabKey: 'appInfo',
        content: <div className="application information" />,
      },
      {
        tabTitle: 'application member',
        tabKey: 'appMember',
        content: <div className="application member" />,
      },
    ],
  },
];
const dataSource2 = [
  {
    tabTitle: 'application information',
    tabKey: 'appInfo',
    content: <div className="application information" />,
  },
  {
    tabTitle: 'application member',
    tabKey: 'appMember',
    content: <div className="application member" />,
  },
];

const routerData = {
  query: {
    tabKey: 'appInfo',
  },
};

describe('SettingTabs', () => {
  beforeAll(() => {
    jest.mock('core/stores/route');
    routeInfoStore.useStore = (fn) => {
      return fn(routerData);
    };
  });
  afterAll(() => {
    jest.resetAllMocks();
  });
  it('render with tabGroup', () => {
    const spy = jest.spyOn(utils, 'updateSearch').mockImplementation();
    const wrapper = mount(<SettingTabs dataSource={dataSource1} className={'class-name'} />);
    expect(wrapper.find('li')).toHaveLength(dataSource1[0].tabGroup.length);
    wrapper.find('li').at(1).simulate('click');
    expect(spy).toHaveBeenCalled();
  });
  it('render without tabGroup', () => {
    const wrapper = mount(<SettingTabs dataSource={dataSource2} />);
    expect(wrapper.find('li')).toHaveLength(dataSource2.length);
  });
});

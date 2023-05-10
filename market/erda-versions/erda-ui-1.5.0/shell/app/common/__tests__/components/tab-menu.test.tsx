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

import { Menu } from 'common';
import React from 'react';
import { mount } from 'enzyme';
import * as utils from 'common/utils/go-to';

const menus = [
  { key: 'create', name: 'CREATE' },
  { key: 'delete', name: 'DELETE' },
  { key: 'update', name: 'UPDATE' },
  { key: 'query', name: 'QUERY' },
];

describe('Menu', () => {
  it('Menu should render empty', () => {
    const wrapper = mount(<Menu activeKey={'activeKey'} />);
    expect(wrapper.find('PureMenu')).toBeEmptyRender();
  });
  it('should ', async () => {
    const goToSpy = jest.spyOn(utils, 'goTo').mockImplementation();
    const beforeTabChangeFn = jest.fn().mockResolvedValue(true);
    const wrapper = mount(
      <Menu activeKey={'delete'} menus={menus} beforeTabChange={beforeTabChangeFn} ignoreTabQuery />,
    );
    expect(wrapper.find('.tab-menu-item')).toHaveLength(menus.length);
    await wrapper.find('.tab-menu-item').at(1).simulate('click');
    expect(goToSpy).not.toHaveBeenCalled();
    await wrapper.find('.tab-menu-item').at(0).simulate('click');
    expect(goToSpy).toHaveBeenLastCalledWith('/erda/dop/apps/create');
    wrapper.setProps({
      beforeTabChange: undefined,
      ignoreTabQuery: undefined,
      keepTabQuery: 'id',
    });
    wrapper.find('.tab-menu-item').at(0).simulate('click');
    expect(goToSpy).toHaveBeenLastCalledWith('/erda/dop/apps/create?id=1');
    goToSpy.mockReset();
  });
});

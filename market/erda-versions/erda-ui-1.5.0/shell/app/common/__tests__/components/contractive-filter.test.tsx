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
import { mount } from 'enzyme';
import { ContractiveFilter } from 'common';
import { ICondition } from 'common/components/contractive-filter';

const genderMap = [
  {
    name: 'male',
    id: 'male',
  },
  {
    name: 'female',
    id: 'female',
  },
];

const conditionsFilter: ICondition[] = [
  {
    key: 'name',
    type: 'input',
    label: 'NAME',
    fixed: true,
    customProps: {
      className: 'filter-condition-item',
    },
  },
  {
    key: 'name',
    type: 'input',
    label: 'NAME',
    fixed: false,
    customProps: {
      className: 'filter-condition-item',
    },
  },
  {
    key: 'gender',
    type: 'select',
    label: 'GENDER',
    fixed: true,
    options: genderMap.map((item) => ({ label: item.name, value: item.id, icon: '' })),
    customProps: {
      className: 'filter-condition-item',
    },
  },
  {
    key: 'birthday',
    fixed: false,
    type: 'dateRange',
    label: 'BIRTHDAY',
    customProps: {
      className: 'filter-condition-item',
    },
  },
];

const fixedCondition = conditionsFilter.filter((t) => t.fixed);
const dynamicCondition = conditionsFilter.filter((t) => !t.fixed);

// processing
describe('ContrastiveFilter', () => {
  it('ContrastiveFilter should work well', () => {
    document.body.innerHTML = '<div id="main" style="height: 400px; overflow-y: auto"></div>';
    const event = new Event('click', { bubbles: true, cancelable: false });
    const changeFn = jest.fn();
    const divEle = document.getElementById('main');
    const wrapper = mount(<ContractiveFilter delay={500} conditions={conditionsFilter} onChange={changeFn} />, {
      attachTo: divEle,
    });
    expect(wrapper.find('.contractive-filter-bar')).toExist();
    expect(wrapper.find('FilterItem')).toHaveLength(fixedCondition.length);
    wrapper.find('.more-conditions').simulate('click');
    expect(wrapper.find('.ant-dropdown-menu-item')).toExist();
    const moreSelectItem = wrapper.find('.ant-dropdown-menu-item').not('.not-select');
    expect(moreSelectItem).toHaveLength(dynamicCondition.length);
    expect(wrapper.find('.contractive-filter-item-close')).not.toExist();
    moreSelectItem.at(1).simulate('click');
    expect(wrapper.find('FilterItem')).toHaveLength(fixedCondition.length + 1);
    expect(wrapper.find('.contractive-filter-item-close')).toExist();
    wrapper.find('.contractive-filter-item-close').simulate('click');
    expect(wrapper.find('FilterItem')).toHaveLength(fixedCondition.length);
    wrapper.find('.more-conditions').simulate('click');
    moreSelectItem.at(1).simulate('click');
    expect(wrapper.find('FilterItem')).toHaveLength(fixedCondition.length + 1);
    wrapper.find('.fake-link').simulate('click');
    expect(wrapper.find('FilterItem')).toHaveLength(fixedCondition.length);
    divEle?.dispatchEvent(event);
    wrapper.unmount();
  });
});

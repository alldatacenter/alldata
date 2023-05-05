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
import { MultiSelect } from 'common';
import { mount } from 'enzyme';
import { get } from 'lodash';
import { act } from 'react-dom/test-utils';

const data = [
  {
    key: 'name',
    type: 'select',
    showSearchByValueAndLabel: true,
    allowInput: true,
    options: [
      { value: '1', label: 'NAME:DEV', text: 'name-erda-1' },
      { value: '2', label: 'NAME:TEST', text: 'name-erda-2' },
      { value: '3', label: 'NAME:STAGING', text: 'name-erda-3' },
    ],
  },
  {
    key: 'org',
    type: 'cascader',
    showSearchByValue: true,
    options: [
      { value: '1', label: 'ORG:DEV', text: 'org-erda-1' },
      { value: '2', label: 'ORG:TEST', text: 'org-erda-2' },
      { value: '3', label: 'ORG:STAGING', text: 'org-erda-3' },
    ],
  },
  {
    key: 'env',
    type: 'cascader',
    showSearchByValue: false,
    options: [
      { value: '1', label: 'ENV:DEV', text: 'org-erda-1' },
      { value: '2', label: 'ENV:TEST', text: 'org-erda-2' },
      { value: '3', label: 'ENV:STAGING', text: 'org-erda-3' },
    ],
  },
];

describe('MultiSelect', () => {
  it('should render with illegal type', () => {
    const wrapper = mount(<MultiSelect data={[{ type: 'input' }]} />);
    expect(wrapper.find('SingleSelect')).toBeEmptyRender();
  });
  it('should render with empty data', () => {
    const wrapper = mount(<MultiSelect />);
    expect(wrapper.find('.flex').html()).toBe('<div class="flex justify-between items-center"></div>');
  });
  it('should render normally', () => {
    const onChangeNameFn = jest.fn();
    const onChangeOrgFn = jest.fn();
    const onChangeEnvFn = jest.fn();
    const onChangeFn = jest.fn();
    let name = get(data, [0, 'options', 0, 'value']);
    const org = get(data, [1, 'options', 0, 'value']);
    const env = get(data, [2, 'options', 0, 'value']);
    const wrapper = mount(
      <MultiSelect
        data={data}
        onChangeMap={{
          name: onChangeNameFn,
          org: onChangeOrgFn,
          env: onChangeEnvFn,
        }}
        value={{
          name,
          org,
          env,
        }}
        onChange={onChangeFn}
      />,
    );
    expect(wrapper.find('SingleSelect').at(0).find('Select')).toExist();
    expect(wrapper.find('SingleSelect').at(1).find('Cascader')).toExist();
    expect(wrapper.find('Select').at(0).prop('value')).toBe(name);
    act(() => {
      name = get(data, [0, 'options', 1, 'value']);
      wrapper.find('Select').at(0).prop('onSelect')(name);
    });
    expect(onChangeNameFn).toHaveBeenLastCalledWith(name);
    expect(onChangeFn).toHaveBeenLastCalledWith({ name, org, env });
    act(() => {
      name = get(data, [0, 'options', 2, 'value']);
      wrapper.find('Select').at(0).prop('onSearch')(name);
    });
    expect(onChangeNameFn).toHaveBeenLastCalledWith(name);
    expect(onChangeFn).toHaveBeenLastCalledWith({ name, org, env });
    act(() => {
      wrapper.find('Select').at(0).prop('onBlur')();
    });
    expect(wrapper.find('Cascader').at(0).prop('showSearch').filter('dev', data[1].options)).toBeTruthy();
  });
});

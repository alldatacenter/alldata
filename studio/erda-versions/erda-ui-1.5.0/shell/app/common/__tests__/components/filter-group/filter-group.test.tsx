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
import { FilterCore } from 'common/components/filter-group';
import { Input } from 'antd';
import { FilterGroup, ToolBarWithFilter, FilterBarHandle } from 'common/components/filter-group';
import { mount } from 'enzyme';
import { act } from 'react-dom/test-utils';
import * as utils from 'common/utils/query-string';
import routeInfoStore from 'core/stores/route';

const list = [
  {
    label: 'name',
    name: 'name',
    type: 'input',
    className: 'name-comp',
    placeholder: 'please enter name',
  },
  {
    label: 'nickName',
    name: 'nickName',
    type: 'select',
    mode: 'multiple',
    className: 'nickName-comp',
    options: [
      { value: '1', name: 'Tom' },
      { value: '2', name: 'Jerry' },
    ],
  },
  {
    label: 'id',
    name: 'id',
    type: 'inputNumber',
    className: 'id-comp',
    placeholder: 'please enter id',
  },
  {
    label: 'org',
    name: 'org',
    type: 'custom',
    Comp: <div className="org-custom-comp" />,
  },
  {
    label: 'env',
    name: 'env',
    type: 'custom',
    getComp: (wrapOnChange) => {
      return (
        <Input
          onChange={(e) => {
            wrapOnChange(e.target.value);
          }}
          className="env-custom-comp"
        />
      );
    },
  },
];

const simpleList = [
  {
    label: 'name',
    name: 'name',
    type: 'input',
    className: 'name-comp',
    placeholder: 'please enter name',
  },
];

const routerData = {
  query: {
    [FilterBarHandle.filterDataKey]: 'name[erda]',
  },
};

describe('filter-group', () => {
  beforeAll(() => {
    jest.mock('core/stores/route');
    routeInfoStore.useStore = (fn) => {
      return fn(routerData);
    };
  });
  afterAll(() => {
    jest.resetAllMocks();
  });
  it('FilterCore should render well', () => {
    const changeFn = jest.fn();
    const spy = jest.spyOn(utils, 'updateSearch').mockImplementation();
    const wrapper = mount(
      <FilterCore list={list} onChange={changeFn} syncUrlOnSearch>
        {({ CompList, resetButton, searchButton, search, reset }) => {
          return (
            <div>
              <div className="comp-list">{CompList}</div>
              <div className="reset-button">{resetButton}</div>
              <div className="search-button">{searchButton}</div>
              <div className="search-btn" onClick={search}>
                search
              </div>
              <div className="reset-btn" onClick={reset}>
                reset
              </div>
            </div>
          );
        }}
      </FilterCore>,
    );
    expect(wrapper.find('.comp-list').children()).toHaveLength(list.length);
    wrapper
      .find('.env-custom-comp')
      .at(0)
      .simulate('change', { target: { value: 'erda.cloud' } });
    expect(changeFn).toHaveBeenCalledTimes(1);
    act(() => {
      wrapper.find('Select').prop('onChange')();
    });
    expect(changeFn).toHaveBeenCalledTimes(2);
    act(() => {
      wrapper.find('Select').prop('onChange')(['1'], { props: { children: 'Tom' } });
    });
    expect(changeFn).toHaveBeenCalledTimes(3);
    act(() => {
      wrapper.find('InputNumber').at(0).prop('onChange')(12);
    });
    expect(changeFn).toHaveBeenCalledTimes(4);
    wrapper
      .find('input.name-comp')
      .at(0)
      .simulate('change', { target: { value: 'ERDA' } });
    expect(changeFn).toHaveBeenCalledTimes(5);
    expect(changeFn).toHaveBeenLastCalledWith({
      _Q_: 'env[erda.cloud]',
      env: 'erda.cloud',
      id: 12,
      name: 'ERDA',
      nickName: undefined,
      org: undefined,
    });
    wrapper.find('.search-btn').simulate('click');
    expect(spy).toHaveBeenCalledTimes(1);
    wrapper.find('.reset-btn').simulate('click');
    expect(spy).toHaveBeenLastCalledWith({
      env: undefined,
      id: undefined,
      name: undefined,
      nickName: undefined,
      org: undefined,
    });
  });
  it('FilterGroup should render well', () => {
    const resetFn = jest.fn();
    const searchFn = jest.fn();
    const wrapper = mount(<FilterGroup list={simpleList} reversePosition onReset={resetFn} onSearch={searchFn} />);
    expect(wrapper.find('.filter-group-left').find('Button')).toExist();
    expect(wrapper.find('.filter-group-right').find('.name-comp')).toExist();
    wrapper.setProps({
      reversePosition: false,
    });
    expect(wrapper.find('.filter-group-right').find('Button')).toExist();
    expect(wrapper.find('.filter-group-left').find('.name-comp')).toExist();
  });
  it('ToolBarWithFilter should work well', () => {
    const ref = React.createRef();
    const searchFn = jest.fn();
    const wrapper = mount(
      <ToolBarWithFilter list={simpleList} ref={ref} onSearch={searchFn} filterValue={{ name: 'erda' }}>
        <ToolBarWithFilter.FilterButton btnClassName="filter-btn" />
      </ToolBarWithFilter>,
    );
    wrapper.find('.filter-btn').at(0).simulate('click');
    expect(wrapper.find('FilterGroup.Drawer').prop('visible')).toBeTruthy();
    act(() => {
      ref.current.onSearchWithFilterBar({});
    });
    expect(searchFn).toHaveBeenCalled();
    act(() => {
      ref.current.onSearchWithFilterBar({ name: 'erda' });
    });
    expect(searchFn).toHaveBeenLastCalledWith({
      _Q_: 'name[erda]',
      name: 'erda',
    });
    wrapper.update();
    expect(wrapper.find('FilterGroup.Drawer').prop('visible')).toBeFalsy();
    wrapper.find('.ant-tag-close-icon').at(0).simulate('click');
    expect(searchFn).toHaveBeenCalledTimes(3);
    wrapper.find('.clear').simulate('click');
    expect(searchFn).toHaveBeenCalledTimes(4);
  });
  it('should FilterBarHandle work well', () => {
    const queryStr = 'name[erda]||org[erda.cloud]';
    const data = { name: 'erda', org: 'erda.cloud' };
    expect(FilterBarHandle.queryToData(queryStr)).toStrictEqual(data);
    expect(FilterBarHandle.dataToQuery(data)).toBe(queryStr);
  });
});

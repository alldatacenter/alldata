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
import { Filter } from 'common';
import { mount, shallow } from 'enzyme';
import { Input } from 'antd';
import * as utils from 'common/utils/query-string';
import _ from 'lodash';

const filterField = [
  {
    type: Input,
    name: 'title',
    customProps: {
      className: 'title-input',
      placeholder: 'filter by title',
    },
  },
  {
    type: Input,
    name: 'name',
    customProps: {
      className: 'name-input',
      placeholder: 'filter by name',
    },
  },
];

describe('filter', () => {
  beforeAll(() => {
    jest.mock('lodash');
    _.debounce = (fn: Function) => fn;
  });
  afterAll(() => {
    jest.resetAllMocks();
  });
  it('Filter should work well', () => {
    // const filterFn = jest.fn();
    const spy = jest.spyOn(utils, 'updateSearch').mockImplementation();
    const wrapper = shallow(<Filter config={filterField} />);
    wrapper.find('PureFilter').prop('updateSearch')();
    expect(spy).toHaveBeenCalled();
    spy.mockReset();
  });
  it('Filter.Pure should work well without connectUrlSearch', () => {
    jest.useFakeTimers();
    const query = { name: 'erda', title: 'erda-cloud' };
    const filterFn = jest.fn();
    const updateSearchFn = jest.fn();
    const wrapper = mount(<Filter.Pure onFilter={filterFn} updateSearch={updateSearchFn} config={filterField} />);
    jest.advanceTimersByTime(2000);
    expect(filterFn).toHaveBeenLastCalledWith({ name: undefined, title: undefined });
    // wrapper
    //   .find('.title-input')
    //   .at(0)
    //   .simulate('change', { target: { value: query.title } });
    // expect(filterFn).toHaveBeenLastCalledWith({ name: undefined, title: query.title });
    // wrapper
    //   .find('.name-input')
    //   .at(0)
    //   .simulate('change', { target: { value: query.name } });
    // expect(filterFn).toHaveBeenLastCalledWith(query);
    wrapper.setProps({
      urlExtra: {
        pageNo: 1,
        ...query,
      },
    });
    expect(updateSearchFn).toHaveBeenLastCalledWith({ ...query, pageNo: 1 });
  });
  it('PureFilter should work well with connectUrlSearch', () => {
    jest.useFakeTimers();
    const query = { name: 'erda', title: 'erda-cloud' };
    const filterFn = jest.fn();
    const updateSearchFn = jest.fn();
    mount(
      <PureFilter
        onFilter={filterFn}
        updateSearch={updateSearchFn}
        config={filterField}
        connectUrlSearch
        query={query}
      />,
    );
    jest.advanceTimersByTime(2000);
    expect(filterFn).toHaveBeenLastCalledWith(query);
    expect(updateSearchFn).toHaveBeenLastCalledWith(query);
  });
});

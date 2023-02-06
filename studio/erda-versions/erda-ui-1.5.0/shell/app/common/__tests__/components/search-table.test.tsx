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
import { SearchTable } from 'common';
import { shallow } from 'enzyme';

const searchValue = 'erda';

describe('SearchTable', () => {
  it('should render well', (done) => {
    const fn = jest.fn();
    const wrapper = shallow(
      <SearchTable
        placeholder="search"
        onSearch={fn}
        triggerByEnter
        extraItems={<div className="extraItems">extraItems</div>}
      >
        <div className="search_table">search table children</div>
      </SearchTable>,
    );
    expect(wrapper.find('.search_table')).toExist();
    wrapper.find('Search').simulate('change', { target: { value: searchValue } });
    expect(fn).toHaveBeenCalledTimes(0);
    wrapper.find('Search').prop('onPressEnter')();
    expect(fn).toHaveBeenLastCalledWith(wrapper.state()?.searchValue);
    wrapper.setProps({
      triggerByEnter: false,
    });
    wrapper.find('Search').simulate('change', { target: { value: searchValue } });
    expect(fn).toHaveBeenLastCalledWith(searchValue);
    wrapper.find('Search').prop('onSearch')(searchValue);
    expect(fn).toHaveBeenLastCalledWith(searchValue);
    wrapper.find('Search').prop('onPressEnter')();
    expect(fn).toHaveBeenCalledTimes(3);
    wrapper.setProps({
      needDebounce: true,
    });
    wrapper.find('Search').simulate('change', { target: { value: 'erda.cloud' } });
    // delay 500 ms, assert whether debounceSearch is executed
    setTimeout(() => {
      expect(fn).toHaveBeenLastCalledWith('erda.cloud');
      done();
    }, 500);
    wrapper.setProps({
      triggerByEnter: true,
    });
  });
  it('should render with ', () => {
    const fn = jest.fn();
    const wrapper = shallow(
      <SearchTable
        searchFullWidth
        searchPosition="right"
        extraPosition="right"
        searchListOps={{
          list: [],
          onUpdateOps: fn,
        }}
      >
        <div className="search_table">search table children</div>
      </SearchTable>,
    );
    expect(wrapper.find('.extra-items-right')).toExist();
    expect(wrapper.find('OperationBar')).toExist();
  });
});

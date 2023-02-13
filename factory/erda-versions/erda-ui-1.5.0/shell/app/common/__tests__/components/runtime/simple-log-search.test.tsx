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
import LogSearchForm from 'common/components/simple-log/simple-log-search';
import { mount } from 'enzyme';

describe('LogSearchForm', () => {
  it('should ', () => {
    const setSearch = jest.fn();
    const wrapper = mount(<LogSearchForm setSearch={setSearch} />);
    wrapper.find('input').simulate('change', { target: { value: '123' } });
    wrapper.find('ForwardRef(Form)').prop('onFinish')();
    expect(setSearch).toHaveBeenLastCalledWith({ requestId: '123' });
    wrapper.setProps({
      formData: {
        requestId: 12345,
      },
    });
    expect(wrapper.find('input').prop('value')).toBe(12345);
  });
});

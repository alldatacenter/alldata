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
import { SimpleLog } from 'common';
import { shallow } from 'enzyme';

describe('SimpleLog', () => {
  it('render without props', () => {
    const wrapper = shallow(<SimpleLog />);
    expect(wrapper.state()).toStrictEqual({ query: {} });
    expect(wrapper.find('SimpleLog.Roller').prop('query')).toStrictEqual({});
    expect(wrapper.find('LogSearchForm').prop('formData')).toStrictEqual({});
  });
  it('render with props', () => {
    const props = {
      requestId: 1,
      applicationId: 2,
    };
    const newSearch = {
      requestId: 123,
    };
    const wrapper = shallow(<SimpleLog {...props} />);
    expect(wrapper.state()).toStrictEqual({ query: props });
    expect(wrapper.find('SimpleLog.Roller').prop('query')).toStrictEqual(props);
    expect(wrapper.find('LogSearchForm').prop('formData')).toStrictEqual(props);
    wrapper.find('LogSearchForm').prop('setSearch')(newSearch);
    expect(wrapper.state()).toStrictEqual({ query: newSearch });
    expect(wrapper.find('SimpleLog.Roller').prop('query')).toStrictEqual(newSearch);
    expect(wrapper.find('LogSearchForm').prop('formData')).toStrictEqual(newSearch);
  });
});

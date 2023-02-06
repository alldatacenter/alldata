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
import { DebounceSearch } from 'common';
import { mount } from 'enzyme';
import { act } from 'react-dom/test-utils';

describe('DebounceSearch', () => {
  it('should render well', () => {
    const fn = jest.fn();
    jest.useFakeTimers();
    const wrapper = mount(<DebounceSearch onChange={fn} />);
    act(() => {
      wrapper.find('Search').prop('onChange')({ target: { value: 'erda' } });
    });
    jest.runAllTimers();
    expect(fn).toHaveBeenLastCalledWith('erda');
  });
});

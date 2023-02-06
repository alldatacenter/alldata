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
import { JsonChecker } from 'common';
import { mount } from 'enzyme';

describe('JsonChecker', () => {
  it('should ', () => {
    const jsonString = JSON.stringify(
      {
        data: {
          name: 'erda',
        },
      },
      null,
      2,
    );
    const onToggleFn = jest.fn();
    const wrapper = mount(<JsonChecker jsonString={jsonString} onToggle={onToggleFn} />);
    wrapper.find('button').simulate('click');
    expect(wrapper.find('pre').text()).toBe(jsonString);
    expect(onToggleFn).toHaveBeenLastCalledWith(true);
    expect(wrapper.find('Copy').prop('opts').text()).toBe(jsonString);
    wrapper.setProps({
      onToggle: undefined,
    });
    wrapper.find('button.ant-btn-background-ghost').simulate('click');
  });
});

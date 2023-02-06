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
import { ProtocolInput } from 'common';
import { mount } from 'enzyme';

describe('ProtocolInput', () => {
  it('should ', () => {
    const onChange = jest.fn();
    const wrapper = mount(<ProtocolInput value="http://www.erda.cloud" onChange={onChange} />);
    expect(wrapper.find('Select').at(0).props().value).toBe('http://');
    expect(wrapper.find('Input').at(0).props().value).toBe('www.erda.cloud');
    wrapper.setProps({
      value: 'https://www.erda.cloud',
    });
    expect(wrapper.find('Select').at(0).props().value).toBe('https://');
    wrapper.find('Select').at(0).props().onChange('http://');
    expect(onChange).toHaveBeenLastCalledWith('http://www.erda.cloud');
    wrapper
      .find('Input')
      .at(0)
      .props()
      .onChange({ target: { value: 'erda.cloud' } });
    expect(onChange).toHaveBeenLastCalledWith('https://erda.cloud');
  });
});

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
import { EditCategory } from 'common/components/tree-category/edit-category';
import { message } from 'antd';
import { mount, shallow, ReactWrapper } from 'enzyme';
import { act } from 'react-dom/test-utils';

const asyncAct = async (wrapper: ReactWrapper) => {
  await act(async () => {
    // KeyCode 13 is Enter
    await wrapper.find('Input').simulate('keydown', { keyCode: 13 });
  });
};

describe('EditCategory', () => {
  it('should work well when contentOnly is false', () => {
    const summitFn = jest.fn().mockResolvedValue();
    const hideFn = jest.fn();
    const wrapper = shallow(<EditCategory defaultName="erda" onSubmit={summitFn} onHide={hideFn} />);
    expect(wrapper.find('.dice-edit-category')).not.toExist();
    wrapper.find('Button').simulate('click');
    expect(summitFn).toHaveBeenLastCalledWith({ name: 'erda' });
  });
  it('should work well when contentOnly is true', async () => {
    const summitFn = jest.fn().mockResolvedValue();
    const hideFn = jest.fn();
    const spyWarning = jest.spyOn(message, 'warning');
    const spyError = jest.spyOn(message, 'error');
    const str = 'erda.cloud';
    const wrapper = mount(<EditCategory contentOnly defaultName="erda" onSubmit={summitFn} onHide={hideFn} />);
    wrapper.find('Input').simulate('change', { target: { value: str } });
    expect(wrapper.find('Input').prop('value')).toBe(str);
    await asyncAct(wrapper);
    expect(summitFn).toHaveBeenLastCalledWith({ name: str });
    wrapper.find('Input').simulate('change', { target: { value: str.repeat(10) } });
    await asyncAct(wrapper);
    expect(spyWarning).toHaveBeenCalledTimes(1);
    wrapper.find('Input').simulate('change', { target: { value: `${str}/` } });
    await asyncAct(wrapper);
    expect(spyError).toHaveBeenCalledTimes(1);
    wrapper.find('Input').simulate('change', { target: { value: '' } });
    await asyncAct(wrapper);
    expect(spyWarning).toHaveBeenCalledTimes(2);
    wrapper.find('#dice-edit-category').simulate('click');
    const event = new Event('click');
    Object.defineProperty(event, 'path', {
      value: [{ id: 'root' }],
    });
    document.body.dispatchEvent(event);
    expect(hideFn).toHaveBeenCalled();
    wrapper.unmount();
    spyWarning.mockReset();
    spyError.mockReset();
  });
});

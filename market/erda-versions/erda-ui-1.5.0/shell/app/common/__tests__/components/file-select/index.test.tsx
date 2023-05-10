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
import { FileSelect } from 'common';
import { mount } from 'enzyme';
import { act } from 'react-dom/test-utils';

describe('FileSelect', () => {
  it('FileSelect should work well', () => {
    const fn = jest.fn();
    const beforeUpload = jest.fn();
    const wrapper = mount(<FileSelect visible onChange={fn} beforeUpload={beforeUpload} />);
    wrapper.find('input').simulate('change', {
      target: {
        files: [{ file: 'foo.png', name: 'foo.png' }],
      },
    });
    expect(beforeUpload).toBeCalled();
    act(() => {
      wrapper.find('Upload').at(0).prop('customRequest')({ file: { name: 'foo.png' } });
      expect(fn).toHaveBeenLastCalledWith({ name: 'foo.png' });
    });
    wrapper.setProps({
      beforeUpload: undefined,
      visible: false,
    });
    expect(wrapper.find('Upload').at(0).prop('beforeUpload')()).toBeTruthy();
  });
});

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
import { Copy } from 'common';
import { mount } from 'enzyme';
import { message } from 'antd';

const copyText = 'hello world';
describe('Copy', () => {
  it('render copy with string children', () => {
    const spySuccess = jest.spyOn(message, 'success');
    const spyError = jest.spyOn(message, 'error');
    const onSuccess = jest.fn();
    const onError = jest.fn();
    const getAttribute = jest.fn().mockReturnValue(copyText);
    const clearSelection = jest.fn();
    const trigger = {
      getAttribute,
    };
    const wrapper = mount(
      <Copy selector="for_copy-select" className="cursor-copy" copyText="Copy" onError={onError} onSuccess={onSuccess}>
        copy
      </Copy>,
    );
    wrapper.setProps({
      copyText,
    });
    expect(wrapper.find('span.cursor-copy').length).toEqual(1);
    expect(wrapper.find('span.cursor-copy').prop('data-clipboard-text')).toBe(copyText);
    wrapper.find('Copy').instance().clipboard.emit('error', { trigger });
    expect(onError).toHaveBeenCalled();
    expect(spyError).toHaveBeenCalled();
    wrapper.find('Copy').instance().clipboard.emit('success', { trigger, clearSelection });
    expect(onSuccess).toHaveBeenCalled();
    expect(clearSelection).toHaveBeenCalled();
    expect(spySuccess).toHaveBeenCalled();
    spySuccess.mockReset();
    spyError.mockReset();
    wrapper.unmount();
  });
});

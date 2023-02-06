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
import { MarkdownEditor } from 'common';
import { shallow } from 'enzyme';

describe('MarkdownEditor', () => {
  it('MarkdownEditor should worl fine', () => {
    const fn = jest.fn();
    const onSubmit = jest.fn();
    const onSetLS = jest.fn();
    const defaultText = 'MarkdownEditor text';
    const longText = 'i am erda MarkdownEditor';
    const maxLength = 20;
    const wrapper = shallow(
      <MarkdownEditor
        value={defaultText}
        onBlur={fn}
        onChange={fn}
        maxLength={maxLength}
        operationBtns={[
          {
            text: 'save',
            type: 'primary',
            onClick: onSubmit
          },
          {
            text: 'cancel',
            onClick: fn,
          },
        ]}
        onFocus={fn}
      />,
    );
    expect(wrapper.find('.markdown-editor-content').children().prop('value')).toBe(defaultText);
    wrapper.find('.markdown-editor-content').children().simulate('blur');
    expect(fn).toHaveBeenLastCalledWith(defaultText);
    // onSubmit
    wrapper.find('Button').at(0).simulate('click');
    expect(fn).toHaveBeenLastCalledWith(defaultText);
    expect(wrapper.find('.markdown-editor-content').children().prop('value')).toBe('');
    wrapper.find('.markdown-editor-content').children().simulate('change', {
      text: defaultText,
    });
    // onSetLS
    wrapper.find('Button').at(1).simulate('click');
    expect(onSetLS).toHaveBeenLastCalledWith(defaultText);
    // onCancel
    wrapper.find('Button').at(2).simulate('click');
    expect(fn).toHaveBeenLastCalledWith();
    wrapper.find('.markdown-editor-content').children().simulate('change', {
      text: longText,
    });
    expect(wrapper.find('.markdown-editor-content').children().prop('value')).toBe(longText.slice(0, maxLength));
    wrapper.setProps({
      value: undefined,
    });
    expect(wrapper.find('.markdown-editor-content').children().prop('value')).toBe('');
  });
});

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
import UploadPlugin from 'common/components/markdown-editor/upload-plugin';
import { mount } from 'enzyme';

const assertMessage = (assert: Function, msg: string) => {
  document.getElementsByClassName('ant-message-notice-content')[0]?.remove();
  assert();
  expect(document.getElementsByClassName('ant-message-notice-content')[0].innerHTML).toContain(msg);
};

describe('UploadPlugin', () => {
  const fn = jest.fn();
  it('UploadPlugin should work find ', () => {
    const temp = mount(
      <UploadPlugin
        editor={{
          insertText: fn,
        }}
      />,
    );

    const content = temp.find('Popover').prop('content');
    const wrapper = mount(<div>{content}</div>);
    const fileChangeData = {
      success: true,
      err: {
        msg: 'error msg',
      },
      data: {
        name: 'file name',
        size: 100,
        url: 'file/path',
      },
    };
    const imgUpload = wrapper.find('Upload').at(0);
    const fileUpload = wrapper.find('Upload').at(2);
    expect(imgUpload.prop('accept')).toBe('.jpg, .jpeg, .png, .gif');
    expect(fileUpload.prop('accept')).toBe('');
    expect(imgUpload.prop('beforeUpload')({ size: 100 })).toBeTruthy();
    expect(imgUpload.prop('beforeUpload')({ size: 30000000 })).toBeFalsy();
    assertMessage(() => {
      expect(imgUpload.prop('onChange')({ file: { status: 'uploading' } })).toBeUndefined();
    }, 'uploading...');
    expect(imgUpload.prop('onChange')({ file: { status: 'done' } })).toBeUndefined();
    assertMessage(() => {
      expect(
        imgUpload.prop('onChange')({ file: { status: 'done', response: { err: { msg: 'error message' } } } }),
      ).toBeUndefined();
    }, 'error message');
    expect(imgUpload.prop('onChange')({ file: { status: 'done', response: fileChangeData } })).toBeUndefined();
    expect(fn).toHaveBeenLastCalledWith('\n![file name（100）](file/path)\n');
  });
});

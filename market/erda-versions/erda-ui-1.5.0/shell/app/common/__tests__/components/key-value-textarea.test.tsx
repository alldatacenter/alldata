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
import { KeyValueTextArea } from 'common';
import { mount } from 'enzyme';
import { Form } from 'antd';

const Comp = (props) => {
  const [form] = Form.useForm();
  return (
    <Form>
      <KeyValueTextArea {...props} form={form} />
    </Form>
  );
};

const data = 'name: erda\norg: erda.cloud';

const assetValue = async (editor, spy, str, msg) => {
  await editor.find('TextArea').simulate('change', {
    target: {
      value: `${data}\n${str}`,
    },
  });
  expect(spy).toHaveBeenLastCalledWith('async-validator:', [`${msg}`]);
};

describe('KeyValueTextArea', () => {
  let spy;
  beforeAll(() => {
    spy = jest.spyOn(console, 'warn').mockImplementation();
  });
  afterAll(() => {
    spy?.mockReset();
  });
  it('should render well', async () => {
    const fn = jest.fn();
    const wrapper = mount(<Comp validate={fn} data={data} maxLength={10} existKeys={['type']} />);
    const editor = wrapper.find('KeyValueTextArea');
    expect(editor.instance().getTextData()).toBe(data);
    await editor.find('TextArea').simulate('change', {
      target: {
        value: `${data}\nenv:test`,
      },
    });
    editor.update();
    expect(editor.instance().getTextData()).toBe(`${data}\nenv:test`);
    expect(fn).toBeCalled();
    assetValue(editor, spy, ':', '第3行: full-width punctuation not allowed');
    assetValue(editor, spy, ':dev', '第3行: full-width punctuation not allowed');
    assetValue(editor, spy, 'env:', '第3行: lack of english colon');
    assetValue(editor, spy, 'env:  ', '第3行: missing value');
    assetValue(editor, spy, 'name:erda', '第3行: key must be unique');
    assetValue(editor, spy, 'env:development', '第3行: the length of Value must not exceed 10 characters');
    assetValue(editor, spy, 'environment:dev', '第3行: the length of Key must not exceed 10 characters');
    assetValue(editor, spy, 'type:dev', '3line: this configuration already exists');
    editor.find('TextArea').simulate('change', {
      target: {
        value: `${data}`,
      },
    });
  });
});

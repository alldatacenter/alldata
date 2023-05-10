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
import { KeyValueTable } from 'common';
import { Form } from 'antd';
import { mount } from 'enzyme';

const data = {
  env: 'test',
  org: 'erda',
  name: 'erda.cloud',
};

const Comp = (props) => {
  const [form] = Form.useForm();
  return (
    <Form form={form}>
      <KeyValueTable {...props} form={form} />
    </Form>
  );
};

describe('KeyValueTable', () => {
  it('should dealTableData', () => {
    const obj = {
      ...data,
      _tb_key_name: 'CPU',
      _tb_value_name: '2',
    };
    expect(KeyValueTable.dealTableData(obj, 'extra')).toStrictEqual({
      env: 'test',
      extra: {
        CPU: '2',
      },
      name: 'erda.cloud',
      org: 'erda',
    });
    expect(KeyValueTable.dealTableData(obj)).toStrictEqual({
      CPU: '2',
      env: 'test',
      name: 'erda.cloud',
      org: 'erda',
    });
  });
  it('should render with TextArea', async () => {
    const fn = jest.fn();
    const wrapper = mount(<Comp data={data} maxLength={10} isTextArea onChange={fn} />);
    const editor = wrapper.find('KeyValueTable');
    expect(editor.instance().getTableData()).toStrictEqual(data);
    expect(editor.state().dataSource).toHaveLength(3);
    expect(editor.find('TextArea')).toExist();
    editor.find('TextArea').at(0).simulate('blur');
    expect(fn).toHaveBeenCalled();
    await editor.instance().handleAdd();
    expect(editor.state().dataSource).toHaveLength(4);
    editor.find('Tooltip').at(0).prop('onConfirm')();
    expect(editor.state().dataSource).toHaveLength(3);
    wrapper.setProps({
      data: { ...data, displayName: 'erda', userName: 'erda', CPU: '2' },
    });
    wrapper.update();
    expect(editor.state().dataSource).toHaveLength(6);
  });
  it('should render with Input', () => {
    const fn = jest.fn();
    const wrapper = mount(<Comp data={data} maxLength={10} onChange={fn} disableAdd />);
    const editor = wrapper.find('KeyValueTable');
    editor.find('Input').at(0).simulate('blur');
    expect(fn).toHaveBeenCalled();
  });
});

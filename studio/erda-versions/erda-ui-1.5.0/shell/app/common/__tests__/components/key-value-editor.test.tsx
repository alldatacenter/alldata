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
import { KeyValueEditor } from 'common';
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
    <Form>
      <KeyValueEditor {...props} form={form} />
    </Form>
  );
};

describe('KeyValueEditor', () => {
  it('should render well', async () => {
    const wrapper = mount(<Comp dataSource={data} />);
    const editor = wrapper.find('KeyValueEditor');
    expect(wrapper.find('KeyValueTable')).toExist();
    expect(wrapper.find('KeyValueTextArea')).not.toExist();
    expect(editor.instance().getEditData()).toStrictEqual(data);
    await wrapper.find({ size: 'small' }).at(0).prop('onChange')();
    editor.update();
    expect(wrapper.find('KeyValueTable')).not.toExist();
    expect(wrapper.find('KeyValueTextArea')).toExist();
    expect(editor.instance().getEditData()).toStrictEqual(data);
    await wrapper.find({ size: 'small' }).at(0).prop('onChange')();
    editor.update();
    expect(wrapper.find('KeyValueTable')).toExist();
    expect(wrapper.find('KeyValueTextArea')).not.toExist();
  });
});

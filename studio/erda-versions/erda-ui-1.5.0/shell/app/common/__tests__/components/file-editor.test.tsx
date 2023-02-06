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
import { FileEditor } from 'common';
import { mount } from 'enzyme';

const data = {
  name: 'erda',
  org: 'erda.cloud',
};

const dataStr = JSON.stringify(data, null, 2);

describe('FileEditor', () => {
  it('render editable', () => {
    jest.useFakeTimers();
    const fn = jest.fn();
    const extra = <div className="extra-action">extra</div>;
    const wrapper = mount(
      <FileEditor
        className="file-editor"
        readOnly={false}
        autoHeight
        actions={{ copy: true, format: true, extra }}
        value={dataStr}
        onChange={fn}
      />,
    );

    expect(wrapper.find('.extra-action')).toExist();
    expect(wrapper.find({ title: 'copy' })).toExist();
    expect(wrapper.find({ title: 'format' })).toExist();
    wrapper.find({ type: 'sx' }).simulate('click');
    expect(fn).toHaveBeenLastCalledWith(dataStr);
  });
  it('render readOnly', () => {
    const wrapper = mount(<FileEditor fileExtension="md" className="file-editor" readOnly value={dataStr} />);
    expect(wrapper.find('pre').text()).toContain(dataStr);
  });
});

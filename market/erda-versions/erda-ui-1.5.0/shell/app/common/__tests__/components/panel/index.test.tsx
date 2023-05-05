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
import { shallow } from 'enzyme';
import { Panel } from 'common';
import { PanelProps } from 'common/components/panel';

const props: PanelProps = {
  isMultiColumn: false,
  fields: [
    {
      label: '测试label',
      valueKey: 'test-label',
      tips: 'test-tips',
    },
  ],
};

describe('Panel', () => {
  it('should support showName ', () => {
    const wrapper = shallow(<Panel {...props} />);
    expect(wrapper.find('Tooltip')).toExist();
    expect(wrapper.find('.text-black').props().title).toBe(props.fields?.[0].label);
  });
});

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
import { SimplePanel } from 'common';
import { mount } from 'enzyme';

describe('SimplePanel', () => {
  it('should render', () => {
    const wrapper = mount(
      <SimplePanel style={{ height: 100 }} className="erda_panel" title="panel title">
        <div className="panel-child">panel-child</div>
      </SimplePanel>,
    );
    expect(wrapper).toHaveClassName('erda_panel');
    expect(wrapper).toHaveStyle('height', 100);
    expect(wrapper.find('.ec-simple-panel-title').text()).toBe('panel title');
    expect(wrapper.find('.ec-simple-panel-body').children()).toHaveHTML('<div class="panel-child">panel-child</div>');
  });
});

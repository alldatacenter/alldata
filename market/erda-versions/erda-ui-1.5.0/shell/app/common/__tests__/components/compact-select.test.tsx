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
import { CompactSelect } from 'common';
import { shallow } from 'enzyme';

describe('CompactSelect', () => {
  it('should ', () => {
    const wrapper = shallow(
      <CompactSelect title="org-select-title" className="org-select">
        <select name="org">
          <option value="erda">erda</option>
        </select>
      </CompactSelect>,
    );
    expect(wrapper.find('.select-addon-before').text()).toBe('org-select-title');
    expect(wrapper.find('select')).toHaveClassName('org-select');
    expect(wrapper.find('select').prop('name')).toBe('org');
  });
});

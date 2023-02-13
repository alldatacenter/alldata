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
import { CompSwitcher } from 'common';
import { mount } from 'enzyme';

const comps: COMMON.SlideComp[] = [
  {
    getComp(): React.ElementType | JSX.Element | null {
      return <div className="comp-1">1</div>;
    },
    getTitle(): string | JSX.Element {
      return 'erda1';
    },
  },
];

describe('CompSwitcher', () => {
  it('should render comp', () => {
    const wrapper = mount(
      <div>
        <CompSwitcher comps={comps}>
          <div className="comp-switcher-child" />
        </CompSwitcher>
      </div>,
    );
    expect(wrapper.find('.comp-1')).toExist();
    expect(wrapper.find('.comp-switcher-child')).not.toExist();
    wrapper.unmount();
  });
  it('should render children', () => {
    const wrapper = mount(
      <div>
        <CompSwitcher comps={[]}>
          <div className="comp-switcher-child" />
        </CompSwitcher>
      </div>,
    );
    expect(wrapper.find('.comp-switcher-child')).toExist();
  });
});

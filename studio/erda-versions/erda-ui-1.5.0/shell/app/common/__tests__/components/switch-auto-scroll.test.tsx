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
import { SwitchAutoScroll } from 'common';
import { mount } from 'enzyme';

describe('SwitchAutoScroll', () => {
  it('render with toPageTop', () => {
    document.body.innerHTML = '<div id="main" style="height: 400px; overflow-y: auto"></div>';
    const wrapper = mount(<SwitchAutoScroll toPageTop triggerBy={{}} />);
    const main = document.getElementById('main');
    expect(main?.scrollTop).toBe(0);
    expect(wrapper.find('SwitchAutoScroll')).toBeEmptyRender();
  });
  it('should ', () => {
    const scrollIntoView = jest.fn();
    Element.prototype.scrollIntoView = scrollIntoView;
    const wrapper = mount(<SwitchAutoScroll triggerBy={{}} />);
    expect(wrapper.find('div')).toExist();
    expect(scrollIntoView).toHaveBeenCalled();
  });
});

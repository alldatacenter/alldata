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
import { mount } from 'enzyme';
import { CardContainer } from 'common';

describe('card-container', () => {
  it('render should be ok', () => {
    const fn = jest.fn();
    const wrapper = mount(
      <CardContainer
        title="hello"
        tip="hers is tip"
        operation={
          <button id="btn" onClick={fn}>
            btn
          </button>
        }
      >
        <span id="child">children</span>
      </CardContainer>,
    );
    const containerDom = wrapper.find('.ec-card-container');
    expect(containerDom).toHaveHTML('hello');
    expect(containerDom).toHaveHTML('hers is tip');
    expect(containerDom.find('button#btn')).toBeTruthy();
    expect(containerDom.find('span#child')).toBeTruthy();
    containerDom.find('#btn').simulate('click');
    expect(fn).toHaveBeenCalled();
  });
  it('render chart container should be ok', () => {
    const wrapper = mount(
      <CardContainer.ChartContainer title="hello">
        <span id="child">children</span>
      </CardContainer.ChartContainer>,
    );
    const containerDom = wrapper.find('.ec-card-container');
    const chartContainerDom = wrapper.find('.ec-chart-container');
    expect(containerDom).toHaveHTML('hello');
    expect(chartContainerDom.find('span#child')).toBeTruthy();
  });
});

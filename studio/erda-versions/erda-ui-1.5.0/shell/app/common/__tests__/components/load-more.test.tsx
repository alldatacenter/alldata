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
import { LoadMore } from 'common';
import { shallow } from 'enzyme';

describe('LoadMore', () => {
  it('should render well', () => {
    const wrapper = shallow(<LoadMore />);
    expect(wrapper).toBeEmptyRender();
    wrapper.setProps({
      isLoading: true,
    });
    expect(wrapper.find('Spin')).toExist();
  });
  it('should init load', () => {
    const loadFn = jest.fn();
    shallow(<LoadMore triggerBy="scroll" threshold={300} load={loadFn} initialLoad getContainer={() => {}} />);
    expect(loadFn).toHaveBeenCalled();
  });
  it('should work well', () => {
    jest.useFakeTimers();
    document.body.innerHTML = '<div id="main" style="height: 400px; overflow-y: auto"></div>';
    const divEle = document.getElementById('main') as HTMLDivElement;
    const loadFn = jest.fn();
    const wrapper = shallow(<LoadMore load={loadFn} hasMore isLoading={false} />, { attachTo: divEle });
    wrapper.setProps({
      hasMore: false,
      isLoading: true,
    });
    wrapper.setProps({
      hasMore: true,
      isLoading: false,
    });
    expect(loadFn).toHaveBeenCalledTimes(1);
    window.dispatchEvent(new Event('resize'));
    jest.advanceTimersByTime(500);
    divEle.dispatchEvent(new Event('scroll'));
    expect(loadFn).toHaveBeenCalledTimes(2);
    wrapper.unmount();
  });
});

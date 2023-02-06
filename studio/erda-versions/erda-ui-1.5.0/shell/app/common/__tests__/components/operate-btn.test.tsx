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
import { TableActions } from 'common';
import { shallow } from 'enzyme';

describe('OperationBtn', () => {
  const getWrapper = (props = {}, onClick = () => {}) => {
    return shallow(
      <TableActions {...props}>
        <a className="btns btns1" href="" onClick={onClick}>
          btn1
        </a>
        <a className="btns btns2" href="">
          btn1
        </a>
        <a className="btns btns3" href="">
          btn1
        </a>
        <a className="btns btns4" href="">
          btn1
        </a>
        <a className="btns btns5" href="">
          btn1
        </a>
        <a className="btns btns6" href="">
          btn1
        </a>
      </TableActions>,
    );
  };
  it('should render with default props', () => {
    // default Props: {limit: 3}
    const onClick = jest.fn();
    const wrapper = getWrapper({}, onClick);
    expect(wrapper.find({ type: 'more' })).toExist();
    wrapper.find('.btns1').simulate('click');
    expect(onClick).toHaveBeenCalled();
    expect(wrapper.children('.btns')).toHaveLength(2);
  });
  it('should render with customize props', () => {
    const stopPropagation = jest.fn();
    const more = <span className="more-operations">more-operations</span>;
    const wrapper = getWrapper({ limit: 4, ellipses: more, className: 'customize-class' });
    expect(wrapper.find('.more-operations').text()).toBe('more-operations');
    expect(wrapper).toHaveClassName('customize-class');
    expect(wrapper.children('.btns')).toHaveLength(3);
    wrapper.find('.operator-dropdown-wrap').simulate('click', { stopPropagation });
    expect(stopPropagation).toHaveBeenCalledTimes(1);
  });
  it('should render with limit greater than the number of btns', () => {
    const more = <span className="more-operations">more-operations</span>;
    const wrapper = getWrapper({ limit: 8, ellipses: more, className: 'customize-class' });
    expect(wrapper.children('.btns')).toHaveLength(6);
  });
});

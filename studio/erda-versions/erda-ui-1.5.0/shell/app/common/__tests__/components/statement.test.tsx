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
import { IF } from 'common';
import { shallow } from 'enzyme';

describe('IF', () => {
  it('check is bool', () => {
    const wrapper = shallow(
      <IF check={false}>
        <div className="if">if</div>
        <IF.ELSE />
        <div className="else">else</div>
      </IF>,
    );
    expect(wrapper.find('.else')).toExist();
    expect(wrapper.find('.if')).not.toExist();
    wrapper.setProps({ check: true });
    expect(wrapper.find('.if')).toExist();
    expect(wrapper.find('.else')).not.toExist();
  });
  it('check is func', () => {
    const wrapper = shallow(
      <IF check={() => false}>
        <div className="if">if</div>
        <IF.ELSE />
        <div className="else">else</div>
      </IF>,
    );
    expect(wrapper.find('.else')).toExist();
    expect(wrapper.find('.if')).not.toExist();
    wrapper.setProps({ check: () => true });
    expect(wrapper.find('.if')).toExist();
    expect(wrapper.find('.else')).not.toExist();
  });
  it('render only one child', () => {
    const wrapper = shallow(
      <IF check={false}>
        <div className="if">if</div>
      </IF>,
    );
    expect(wrapper).toBeEmptyRender();
    wrapper.setProps({ check: true });
    expect(wrapper.find('.if')).toExist();
  });
  it('render only if', () => {
    const wrapper = shallow(
      <IF check={false}>
        <div className="if1">if</div>
        <div className="if2">if</div>
      </IF>,
    );
    expect(wrapper).toBeEmptyRender();
  });
  it('empty', () => {
    const wrapper = shallow(<IF check={false} />);
    expect(wrapper).toBeEmptyRender();
  });
  it('else', () => {
    const wrapper = shallow(<IF.ELSE />);
    expect(wrapper).toBeEmptyRender();
  });
});

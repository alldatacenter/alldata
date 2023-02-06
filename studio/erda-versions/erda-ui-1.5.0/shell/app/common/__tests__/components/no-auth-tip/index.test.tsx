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
import { NoAuthTip } from 'common';
import { shallow } from 'enzyme';

describe('NoAuthTip', () => {
  it('render with no children', () => {
    const wrapper = shallow(<NoAuthTip />);
    expect(wrapper.children()).not.toExist();
  });
  it('render with children', () => {
    const onClick = jest.fn();
    const wrapper = shallow(
      <NoAuthTip>
        <button onClick={onClick} className="buttons">
          click me
        </button>
      </NoAuthTip>,
    );
    expect(wrapper.find('.buttons')).toHaveClassName('not-allowed');
    expect(wrapper.find('.buttons').prop('disabled')).toBe(true);
    expect(wrapper.find('.buttons').prop('onClick')).toBeFalsy();
    wrapper.find('.buttons').simulate('click');
    expect(onClick).not.toHaveBeenCalled();
  });
});

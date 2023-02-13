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
import { connectCube } from 'common/utils';
import { mount } from 'enzyme';

const Comp = (props = {}) => {
  return <div {...props}>Comp</div>;
};

const WrapComp = (mapper) => connectCube(Comp, mapper);

describe('connectCube', () => {
  it('should work fine', () => {
    const mapper = () => {
      return {
        className: 'connect-cube',
      };
    };
    const C = WrapComp(mapper);
    const wrapper = mount(
      <div>
        <C />
      </div>,
    );
    expect(wrapper.find('.connect-cube')).toExist();
  });
});

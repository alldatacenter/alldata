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
import { UserInfo } from 'common';
import { shallow } from 'enzyme';
import userStore from 'core/stores/userMap';

describe('user-info', () => {
  beforeAll(() => {
    jest.mock('core/stores/userMap');
    userStore.useStore = (fn) => {
      return fn({
        1: {
          name: 'name-dice',
          nick: 'nick-dice',
        },
        2: {
          name: 'name-dice',
        },
        3: {
          nick: 'nick-dice',
        },
      });
    };
  });
  afterAll(() => {
    jest.resetAllMocks();
  });
  it('fullData', () => {
    const wrapper = shallow(<UserInfo id={1} />);
    expect(wrapper.text()).toEqual('nick-dice');
  });
  it('onlyName', () => {
    const wrapper = shallow(<UserInfo id={2} />);
    expect(wrapper.text()).toEqual('name-dice');
  });
  it('onlyNick', () => {
    const wrapper = shallow(
      <UserInfo
        id={3}
        render={(data, id) => {
          return (
            <div className="render-comp">
              {data.name}
              {data.nick}-{id}
            </div>
          );
        }}
      />,
    );
    expect(wrapper.find('.render-comp').length).toEqual(1);
  });
  it('noData', () => {
    const wrapper = shallow(<UserInfo id={4} />);
    expect(wrapper.text()).toEqual('4');
  });
});

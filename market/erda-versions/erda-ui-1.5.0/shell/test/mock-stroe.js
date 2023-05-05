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

import * as C from '../core/cube';

const mockStore = (path, state, effect) => {
  jest.mock('core/stores/userMap', () => {
    return C?.createStore({
      name: 'userMap',
      state: {
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
      },
      reducers: {
        setUserMap(state, userInfo) {
          return { ...state, ...userInfo };
        },
      },
    });
  });
};

export default mockStore;

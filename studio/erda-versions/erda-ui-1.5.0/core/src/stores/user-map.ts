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

import { createStore } from '../cube';

export interface IUserInfo {
  avatar: string;
  email: string;
  id: string;
  lastLoginAt: string;
  name: string;
  nick: string;
  phone: string;
  pwdExpireAt: string;
  source: string;
  token: string;
}
const initState: Record<string, IUserInfo> = {};

const userMap = createStore({
  name: 'coreUserMap',
  state: initState,
  reducers: {
    setUserMap(state, userInfo: Record<string, IUserInfo>) {
      return { ...state, ...userInfo };
    },
  },
});

export const useUserMap = () => userMap.useStore((s) => s);
export const getUserMap = () => userMap.getState((s) => s);
const { setUserMap } = userMap.reducers;
export { setUserMap };

export default userMap;

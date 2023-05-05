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

import { createStore } from 'core/cube';
import { getAuthenticateList } from '../services/authenticate';
import { addBlackList, addErase } from '../services/safety';
import i18n from 'i18n';

interface IState {
  list: PUBLISHER.IAuthenticate[];
}
const initState: IState = {
  list: [],
};

const authenticate = createStore({
  name,
  state: initState,
  effects: {
    async getList({ call, update }, payload: PUBLISHER.IListQuery) {
      const list = await call(getAuthenticateList, payload);
      update({ list });
      return list;
    },
    addBlackList: async ({ call }, payload: { artifactId: string }) => {
      const res = await call(addBlackList, payload, { successMsg: i18n.t('operated successfully') });
      return res;
    },
    addErase: async ({ call }, payload: { artifactId: string }) => {
      const res = await call(addErase, payload, { successMsg: i18n.t('operated successfully') });
      return res;
    },
  },
});

export default authenticate;

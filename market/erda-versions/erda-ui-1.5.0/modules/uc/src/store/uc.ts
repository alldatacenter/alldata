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

import { createFlatStore, history, i18n } from 'src/common';
import { login, registration, logout, whoAmI, updateUser, updatePassword } from 'src/services/uc';

interface State {
  user?: UC.IUser;
}

const initState: State = {
  user: undefined,
};

const ucStore = createFlatStore({
  name: 'erda-uc',
  state: initState,
  effects: {
    async login({ call }, payload: UC.ILoginPayload) {
      const res = await call(login, payload);
      return res;
    },
    async registration({ call }, payload: UC.IRegistrationPayload) {
      const res = await call(registration, payload, { successMsg: i18n.t('registered successfully') });
      return res;
    },
    async logout({ call }) {
      const res = await call(logout);
      history.push('/uc/login');
      return res;
    },
    async whoAmI({ call, update }) {
      const res = await call(whoAmI);
      const resObj = res?.identity?.traits;
      const user = {
        email: resObj?.email,
        nickname: resObj?.nickname,
        id: res?.identity?.id,
        username: resObj.username,
      };
      update({ user });
      return res;
    },
    async updateUser({ call }, payload: Omit<UC.IUser, 'id'>) {
      const res = await call(updateUser, payload, { successMsg: i18n.t('update successfully') });
      await ucStore.whoAmI();
      return res;
    },
    async updatePassword({ call }, payload: string) {
      const res = await call(updatePassword, payload, { successMsg: i18n.t('update successfully') });
      return res;
    },
  },
  reducers: {},
});

export default ucStore;

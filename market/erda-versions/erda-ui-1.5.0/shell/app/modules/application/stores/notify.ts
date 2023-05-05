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
import i18n from 'i18n';
import {
  getNotifyConfigs,
  deleteNotifyConfigs,
  createNotifyConfigs,
  updateNotifyConfigs,
  toggleNotifyConfigs,
  getNotifyItems,
} from '../services/notify';

interface IState {
  notifyConfigs: APP_NOTIFY.INotify[];
  notifyItems: APP_NOTIFY.INotifyItem[];
}

const initState: IState = {
  notifyConfigs: [],
  notifyItems: [],
};

const notify = createStore({
  name: 'app-notify',
  state: initState,
  effects: {
    async getNotifyConfigs({ call, update }, payload: COMMON_NOTIFY.IGetNotifyGroupQuery) {
      const { list } = await call(getNotifyConfigs, payload);
      update({ notifyConfigs: list });
    },
    async deleteNotifyConfigs({ call }, groupId: string | number) {
      await call(deleteNotifyConfigs, groupId, { successMsg: i18n.t('deleted successfully') });
    },
    async createNotifyConfigs({ call }, payload: APP_NOTIFY.IUpdateNotifyQuery) {
      await call(createNotifyConfigs, payload, { successMsg: i18n.t('created successfully') });
    },
    async updateNotifyConfigs({ call }, payload: APP_NOTIFY.IUpdateNotifyQuery) {
      await call(updateNotifyConfigs, payload, { successMsg: i18n.t('updated successfully') });
    },
    async toggleNotifyConfigs({ call }, payload: { id: number; action: string }) {
      await call(toggleNotifyConfigs, payload, { successMsg: i18n.t('updated successfully') });
    },
    async getNotifyItems({ call, update }, payload: { scopeType: string; module: string }) {
      const { list } = await call(getNotifyItems, payload);
      update({ notifyItems: list });
    },
  },
});

export default notify;

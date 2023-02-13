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

import { getAuditList, getAuditLogSetting, updateAuditLogSetting } from '../services/audit';
import { getDefaultPaging } from 'common/utils';
import { createStore } from 'core/cube';
import i18n from 'i18n';
import orgStore from 'app/org-home/stores/org';

interface IState {
  auditList: AUDIT.Item[];
  auditPaging: IPaging;
  setting: {
    interval?: number;
  };
}

const initState: IState = {
  auditList: [],
  auditPaging: getDefaultPaging(),
  setting: {
    interval: undefined,
  },
};

const audit = createStore({
  name: 'audit',
  state: initState,
  effects: {
    async getList({ call, update }, payload: AUDIT.ListQuery) {
      const orgId = payload.sys ? undefined : orgStore.getState((s) => s.currentOrg.id);
      const { list } = await call(getAuditList, { ...payload, orgId }, { paging: { key: 'auditPaging' } });
      update({ auditList: list });
      return list;
    },
    async getAuditLogSetting({ call, update }) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const setting = await call(getAuditLogSetting, orgId);
      update({ setting });
    },
    async updateAuditLogSetting({ call }, payload: AUDIT.LogSettingBody) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      return call(updateAuditLogSetting, { ...payload, orgId }, { successMsg: i18n.t('updated successfully') });
    },
  },
  reducers: {
    clearList(state) {
      state.auditList = [];
      state.auditPaging = getDefaultPaging();
    },
    clearAuditLogSetting(state) {
      state.setting = {
        interval: undefined,
      };
    },
  },
});

export default audit;

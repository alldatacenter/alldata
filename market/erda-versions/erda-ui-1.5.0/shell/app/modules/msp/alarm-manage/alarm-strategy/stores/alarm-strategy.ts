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
import { forEach } from 'lodash';
import {
  createAlert,
  deleteAlert,
  editAlert,
  getAlertDetail,
  getAlerts,
  getAlertTypes,
  toggleAlert,
  getAlertTriggerConditions,
  getAlertTriggerConditionsContent,
} from '../services/alarm-strategy';
import { getApps } from 'common/services';
import i18n from 'i18n';
import { PAGINATION } from 'app/constants';

interface IState {
  alertTypes: COMMON_STRATEGY_NOTIFY.IAlertType;
  alertList: COMMON_STRATEGY_NOTIFY.IAlert[];
  alarmPaging: IPaging;
  alarmScopeMap: { [key: string]: string } | {};
  alertTriggerConditions: COMMON_STRATEGY_NOTIFY.IAlertTriggerCondition[];
  alertTriggerConditionsContent: COMMON_STRATEGY_NOTIFY.IAlertTriggerConditionContent[];
}

const defaultPaging = {
  pageNo: 1,
  pageSize: PAGINATION.pageSize,
  total: 0,
};

const initOrgState: IState = {
  alertTypes: {} as COMMON_STRATEGY_NOTIFY.IAlertType,
  alertList: [],
  alarmScopeMap: {},
  alarmPaging: defaultPaging,
  alertTriggerConditions: [],
  alertTriggerConditionsContent: [],
};

const alarmStrategy = createStore({
  name: 'mspAlarmStrategy',
  state: initOrgState,
  effects: {
    async getAlerts({ call, update, getParams }, payload?: COMMON_STRATEGY_NOTIFY.IPageParam) {
      const { tenantGroup } = getParams();
      const { list = [] } =
        (await call(getAlerts, { ...payload, tenantGroup }, { paging: { key: 'alarmPaging' } })) ?? {};
      update({ alertList: list });
    },
    async getAlertDetail({ call, getParams }, id: number) {
      const { tenantGroup } = getParams();
      const alertDetail = await call(getAlertDetail, { id, tenantGroup });
      return alertDetail;
    },
    async createAlert({ call, getParams }, body: COMMON_STRATEGY_NOTIFY.IAlertBody) {
      const { tenantGroup } = getParams();
      await call(createAlert, { body, tenantGroup }, { successMsg: i18n.t('operated successfully') });
      await alarmStrategy.effects.getAlerts({ pageNo: 1 });
    },
    async editAlert({ call, getParams }, payload: { body: COMMON_STRATEGY_NOTIFY.IAlertBody; id: string }) {
      const { tenantGroup } = getParams();
      await call(editAlert, { ...payload, tenantGroup }, { successMsg: i18n.t('operated successfully') });
      await alarmStrategy.effects.getAlerts({ pageNo: 1 });
    },
    async getAlertTypes({ call, update, getParams }) {
      const { tenantGroup } = getParams();
      const alertTypes = await call(getAlertTypes, tenantGroup);
      update({ alertTypes });
    },
    async toggleAlert({ call, getParams }, { id, enable }) {
      const { tenantGroup } = getParams();
      await call(toggleAlert, { id, enable, tenantGroup });
    },
    async deleteAlert({ call, getParams }, id: number) {
      const { tenantGroup } = getParams();
      await call(deleteAlert, { id, tenantGroup }, { successMsg: i18n.t('operated successfully') });
      await alarmStrategy.effects.getAlerts({ pageNo: 1 });
    },
    async getAlarmScopes({ call, update, getParams }) {
      const { projectId } = getParams();
      const { list } = await call(getApps, { projectId, mode: 'SERVICE', pageSize: 1000 });
      const alarmScopeMap = {};
      forEach(list, ({ id, name }) => {
        alarmScopeMap[id] = name;
      });
      update({ alarmScopeMap });
    },
    async getAlertTriggerConditions({ call, update }, scopeType: string) {
      const alertTriggerConditions = await call(getAlertTriggerConditions, scopeType);
      update({ alertTriggerConditions });
    },
    async getAlertTriggerConditionsContent(
      { call, update },
      payload: COMMON_STRATEGY_NOTIFY.IAlertTriggerConditionQueryItem[],
    ) {
      const alertTriggerConditionsContent = await call(getAlertTriggerConditionsContent, payload);
      update({ alertTriggerConditionsContent });
    },
  },
  reducers: {
    clearAlerts(state) {
      state.alertList = [];
    },
  },
});

export default alarmStrategy;

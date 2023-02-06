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
import { keyBy, map, isEmpty } from 'lodash';
import * as mspCustomAlarmService from 'msp/alarm-manage/alarm-strategy/services/custom-alarm';
import * as orgCustomAlarmService from 'app/modules/cmp/services/custom-alarm';
import i18n from 'i18n';
import { PAGINATION } from 'app/constants';

export enum CustomAlarmScope {
  ORG = 'org',
  MICRO_SERVICE = 'micro-service',
}

export interface IState {
  customAlarms: COMMON_CUSTOM_ALARM.CustomAlarms[];
  customMetricMap: COMMON_CUSTOM_ALARM.CustomMetricMap;
  customAlarmDetail: COMMON_CUSTOM_ALARM.CustomAlarmDetail;
  customAlarmTargets: COMMON_CUSTOM_ALARM.AlarmTarget[];
  customAlarmPaging: IPaging;
}

export const createCustomAlarmStore = (scope: CustomAlarmScope) => {
  const serviceMap = {
    org: orgCustomAlarmService,
    'micro-service': mspCustomAlarmService,
  };
  const {
    getCustomAlarms,
    switchCustomAlarm,
    deleteCustomAlarm,
    getCustomMetrics,
    getCustomAlarmDetail,
    getCustomAlarmTargets,
    createCustomAlarm,
    editCustomAlarm,
    getPreviewMetaData,
  } = serviceMap[scope];

  const defaultPaging = {
    pageNo: 1,
    pageSize: PAGINATION.pageSize,
    total: 0,
  };

  const initState: IState = {
    customAlarms: [],
    customMetricMap: {} as COMMON_CUSTOM_ALARM.CustomMetricMap,
    customAlarmDetail: {} as COMMON_CUSTOM_ALARM.CustomAlarmDetail,
    customAlarmTargets: [],
    customAlarmPaging: defaultPaging,
  };

  const customAlarmStore = createStore({
    name: `${scope}CustomAlarm`,
    state: initState,
    effects: {
      async getCustomAlarms(
        { call, update, getParams },
        payload?: Omit<COMMON_CUSTOM_ALARM.IPageParam, 'tenantGroup'>,
      ) {
        const { tenantGroup } = getParams();
        const { list: customAlarms } = await call(
          getCustomAlarms,
          { ...payload, tenantGroup },
          { paging: { key: 'customAlarmPaging' } },
        );
        update({ customAlarms });
      },
      async getCustomAlarmDetail({ call, update, getParams }, id: number) {
        const { tenantGroup } = getParams();
        const customAlarmDetail: COMMON_CUSTOM_ALARM.CustomAlarmDetail = await call(getCustomAlarmDetail, {
          id,
          tenantGroup,
        });
        update({ customAlarmDetail });
      },
      async switchCustomAlarm({ call, select, getParams }, payload: { id: number; enable: boolean }) {
        const { tenantGroup } = getParams();
        await call(
          switchCustomAlarm,
          { ...payload, tenantGroup },
          { successMsg: i18n.t('status switched successfully') },
        );
        const { pageSize, pageNo } = select((s) => s.customAlarmPaging);
        customAlarmStore.effects.getCustomAlarms({ pageNo, pageSize });
      },
      async deleteCustomAlarm({ call, getParams }, id: number) {
        const { tenantGroup } = getParams();
        await call(deleteCustomAlarm, { id, tenantGroup }, { successMsg: i18n.t('deleted successfully') });
        customAlarmStore.effects.getCustomAlarms();
      },
      async getCustomMetrics({ call, update, getParams }) {
        const { tenantGroup } = getParams();
        const data: COMMON_CUSTOM_ALARM.CustomMetrics = await call(getCustomMetrics, tenantGroup);
        if (isEmpty(data)) return;
        const { metrics, filterOperators, functionOperators, ...restData } = data;
        const metricMap = keyBy(
          map(metrics, (item) => {
            const fieldMap = keyBy(item.fields, 'field.key');
            const tagMap = keyBy(item.tags, 'tag.key');
            return { ...item, fieldMap, tagMap };
          }),
          'name.key',
        );
        const filterOperatorMap = keyBy(filterOperators, 'key');
        const functionOperatorMap = keyBy(functionOperators, 'key');
        update({ customMetricMap: { ...restData, filterOperatorMap, functionOperatorMap, metricMap } });
      },
      async getCustomAlarmTargets({ call, update, getParams }) {
        const { tenantGroup } = getParams();
        const { targets: customAlarmTargets } = await call(getCustomAlarmTargets, tenantGroup);
        update({ customAlarmTargets });
      },
      async createCustomAlarm({ call, getParams }, payload: Omit<COMMON_CUSTOM_ALARM.CustomAlarmQuery, 'tenantGroup'>) {
        const { tenantGroup } = getParams();
        await call(createCustomAlarm, { ...payload, tenantGroup }, { successMsg: i18n.t('created successfully') });
        customAlarmStore.effects.getCustomAlarms();
      },
      async editCustomAlarm({ call, getParams }, payload: Omit<COMMON_CUSTOM_ALARM.CustomAlarmQuery, 'tenantGroup'>) {
        const { tenantGroup } = getParams();
        await call(editCustomAlarm, { ...payload, tenantGroup }, { successMsg: i18n.t('updated successfully') });
        customAlarmStore.effects.getCustomAlarms();
      },
      async getPreviewMetaData(
        { call, getParams },
        payload: Omit<COMMON_CUSTOM_ALARM.CustomAlarmQuery, 'tenantGroup'>,
      ) {
        const { tenantGroup } = getParams();
        const metaData = await call(getPreviewMetaData, { ...payload, tenantGroup });
        return metaData;
      },
    },
    reducers: {
      clearCustomAlarms(state) {
        state.customAlarms = [];
      },
      clearCustomAlarmDetail(state) {
        state.customAlarmDetail = {} as COMMON_CUSTOM_ALARM.CustomAlarmDetail;
      },
    },
  });

  return customAlarmStore;
};

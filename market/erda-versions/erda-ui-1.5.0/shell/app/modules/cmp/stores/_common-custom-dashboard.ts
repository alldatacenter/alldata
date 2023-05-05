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

import i18n from 'i18n';
import { createFlatStore } from 'core/cube';
import { getDefaultPaging, getTimeSpan, qs } from 'common/utils';
import breadcrumbStore from 'app/layout/stores/breadcrumb';

import * as orgCustomDashboardService from 'cmp/services/custom-dashboard';
import * as mspCustomDashboardService from 'msp/query-analysis/custom-dashboard/services/custom-dashboard';
import { ITimeRange, transformRange } from 'common/components/time-select/common';
import moment from 'moment';

export enum CustomDashboardScope {
  ORG = 'org',
  MICRO_SERVICE = 'micro_service',
}

export interface IState {
  customDashboardList: Custom_Dashboard.DashboardItem[];
  customDashboardPaging: IPaging;
  timeSpan: any;
  globalTimeSelectSpan: {
    data: ITimeRange;
    range: {
      triggerTime?: number;
    } & ITimeSpan;
  };
}

const query = qs.parse(location.search);
let defaultRange: ITimeRange = {
  mode: 'quick',
  quick: 'hours:1',
  customize: {},
};

if (query.mode === 'quick' && query.quick) {
  defaultRange = { mode: 'quick', quick: query.quick, customize: {} };
} else if (query.mode === 'customize' && query.start && query.end) {
  defaultRange = { mode: 'customize', quick: '', customize: { start: moment(+query.start), end: moment(+query.end) } };
}
const { date } = transformRange(defaultRange);

export const createCustomDashboardStore = (scope: CustomDashboardScope) => {
  const serviceMap = {
    [CustomDashboardScope.ORG]: orgCustomDashboardService,
    [CustomDashboardScope.MICRO_SERVICE]: mspCustomDashboardService,
  };
  const initState: IState = {
    customDashboardList: [],
    customDashboardPaging: getDefaultPaging(),
    timeSpan: getTimeSpan(),
    globalTimeSelectSpan: {
      data: defaultRange,
      range: {
        triggerTime: 0,
        ...getTimeSpan(date),
      },
    },
  };
  const {
    getCustomDashboard,
    createCustomDashboard,
    deleteCustomDashboard,
    updateCustomDashboard,
    getCustomDashboardDetail,
  } = serviceMap[scope];

  const customDashboardStore = createFlatStore({
    name: `${scope}CustomDashboard`,
    state: initState,
    effects: {
      async createCustomDashboard({ call }, payload: Custom_Dashboard.DashboardItem) {
        await call(createCustomDashboard, payload, { successMsg: i18n.t('cmp:O&M dashboard added successfully') });
      },
      async updateCustomDashboard({ call }, payload: Custom_Dashboard.DashboardItem) {
        await call(updateCustomDashboard, payload, { successMsg: i18n.t('cmp:O&M dashboard updated successfully') });
      },
      async getCustomDashboard({ call, update }, payload: Custom_Dashboard.GetDashboardPayload) {
        const { list } = await call(getCustomDashboard, payload, { paging: { key: 'customDashboardPaging' } });
        update({ customDashboardList: list });
      },
      async getCustomDashboardDetail({ call }, payload: { id: string; scopeId: string }) {
        const customDashboardDetail = await call(getCustomDashboardDetail, payload);
        breadcrumbStore.reducers.setInfo('dashboardName', customDashboardDetail.name);
        return customDashboardDetail;
      },
      async deleteCustomDashboard({ call }, payload: { id: string; scopeId: string }) {
        await call(deleteCustomDashboard, payload, { successMsg: i18n.t('deleted successfully') });
      },
    },
    reducers: {
      updateTimeSpan(state, payload) {
        state.timeSpan = payload;
      },
      resetTimeSpan(state) {
        state.timeSpan = getTimeSpan();
      },
      updateState(state, payload) {
        return { ...state, ...payload };
      },
    },
  });

  return customDashboardStore;
};

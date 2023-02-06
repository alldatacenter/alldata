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
  getProjectDashboard,
  getStatusDetail,
  getPastIncidents,
  getMetricStatus,
  saveService,
  updateMetric,
  deleteMetric,
  setDatumPoint,
} from '../services/status';
import breadcrumbStore from 'app/layout/stores/breadcrumb';

interface IState {
  dashboard: MONITOR_STATUS.IDashboardResp;
  detail: MONITOR_STATUS.IDashboardResp;
  metricStatus: MONITOR_STATUS.IMetrics;
  pastIncidents: MONITOR_STATUS.IPastIncidents[];
}

const initState: IState = {
  dashboard: {},
  detail: {},
  metricStatus: {},
  pastIncidents: [],
};
const Status = createStore({
  name: 'monitorStatus',
  state: initState,
  effects: {
    async getProjectDashboard({ call, update, getParams }) {
      const { projectId, terminusKey, env } = getParams();
      const dashboard = await call(getProjectDashboard, { projectId, tenantId: terminusKey, env });
      if (dashboard) {
        update({ dashboard });
      }
    },
    async getStatusDetail({ call, update, getParams }, payload?: { period: string }) {
      const { metricId } = getParams();
      const detail = await call(getStatusDetail, { ...(payload || {}), id: metricId });
      if (detail) {
        update({ detail });
        breadcrumbStore.reducers.setInfo('monitorStatusDetail', detail);
      }
    },
    async getPastIncidents({ call, update, getParams }) {
      const { metricId } = getParams();
      const pastIncidents = await call(getPastIncidents, { id: metricId });
      if (pastIncidents) {
        update({ pastIncidents });
      }
    },
    async getMetricStatus({ call, update, getParams }) {
      const { metricId } = getParams();
      const metricStatus = await call(getMetricStatus, { metricId });
      if (metricStatus) {
        update({ metricStatus });
      }
    },
    async saveService({ call }, payload: MONITOR_STATUS.IMetricsBody) {
      const result = await call(saveService, { data: payload });
      return result;
    },
    async updateMetric({ call }, payload: MONITOR_STATUS.IMetricsBody) {
      const result = await call(updateMetric, payload);
      return result;
    },
    async deleteMetric({ call }, payload: string) {
      const result = await call(deleteMetric, payload, { successMsg: i18n.t('deleted successfully') });
      return result;
    },
    async setDatumPoint({ call }, payload: { id: string; url: string }) {
      await call(setDatumPoint, payload, { successMsg: i18n.t('set successfully'), fullResult: true });
    },
  },
  reducers: {
    clearDashboardInfo(state) {
      state.dashboard = {};
    },
    clearStatusDetail(state) {
      state.detail = {};
    },
    clearMetricStatus(state) {
      state.metricStatus = {};
    },
  },
});

export default Status;

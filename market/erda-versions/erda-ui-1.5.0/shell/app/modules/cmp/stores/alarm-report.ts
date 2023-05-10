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
import orgStore from 'app/org-home/stores/org';
import { getDefaultPaging, goTo } from 'common/utils';
import breadcrumbStore from 'app/layout/stores/breadcrumb';
import {
  createReportTask,
  updateReportTask,
  deleteReportTask,
  switchReportTask,
  getReportTaskRecords,
  getReportTaskRecord,
  getSystemDashboards,
  getReportTasks,
  getReportTypes,
  getReportTask,
  getAlarmReport,
  getCPUAlarmReport,
  getProcessCmdline,
  getSystemReport,
} from '../services/alarm-report';
import i18n from 'i18n';
import { ALARM_REPORT_CHART_MAP } from 'dcos/common/config';

import { PAGINATION } from 'app/constants';

const defaultPagingReq = {
  pageNo: 1,
  pageSize: PAGINATION.pageSize,
};

interface IState {
  alarmReport: {
    [k: string]: any;
    loading: boolean;
  };
  processCmdline: string;
  reportTasks: COMMON_ALARM_REPORT.ReportTask[];
  reportTaskRecords: COMMON_ALARM_REPORT.ReportRecord[];
  reportTaskRecord: COMMON_ALARM_REPORT.ReportRecord;
  systemDashboards: COMMON_ALARM_REPORT.DashboardBlock[];
  reportTypes: COMMON_ALARM_REPORT.ReportType[];
  reportTaskPaging: IPaging;
  reportTaskRecordPaging: IPaging;
}

const initOrgState: IState = {
  alarmReport: {
    loading: false,
  },
  processCmdline: '',
  reportTasks: [],
  reportTaskRecords: [],
  systemDashboards: [],
  reportTypes: [],
  reportTaskRecord: {} as COMMON_ALARM_REPORT.ReportRecord,
  reportTaskPaging: getDefaultPaging(),
  reportTaskRecordPaging: getDefaultPaging(),
};

const alarmReportStore = createStore({
  name: 'cmpAlarmReport',
  state: initOrgState,
  subscriptions({ listenRoute }: IStoreSubs) {
    listenRoute(({ isEntering, query, params }) => {
      if (isEntering('clusterAlarmReport')) {
        const { category, x_filter_host_ip: ip, x_timestamp: timestamp } = query;
        const { clusterName, chartUniqId } = params;
        goTo(goTo.pages.alarmReport, { clusterName, chartUniqId, category, ip, timestamp });
      }
    });
  },
  effects: {
    async createReportTask({ call }, payload: COMMON_ALARM_REPORT.ReportTaskQuery) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      await call(
        createReportTask,
        { scope: 'org', scopeId: String(orgId), ...payload },
        { successMsg: i18n.t('added successfully') },
      );
      alarmReportStore.effects.getReportTasks(defaultPagingReq);
    },
    async getReportTasks({ call, update }, payload: IPagingReq) {
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { list } = await call(
        getReportTasks,
        { scope: 'org', scopeId: String(orgId), ...payload },
        { paging: { key: 'reportTaskPaging' } },
      );
      update({ reportTasks: list });
    },
    async getReportTask({ call, getParams }) {
      const { taskId } = getParams();
      const { name } = await call(getReportTask, taskId);
      breadcrumbStore.reducers.setInfo('opReportName', name);
    },
    async updateReportTask({ call, select }, payload: Merge<COMMON_ALARM_REPORT.ReportTaskQuery, { id: number }>) {
      await call(updateReportTask, payload, { successMsg: i18n.t('updated successfully') });
      const { pageNo, pageSize } = select((s) => s.reportTaskPaging);
      alarmReportStore.effects.getReportTasks({ pageNo, pageSize });
    },
    async deleteReportTask({ call }, id: number) {
      await call(deleteReportTask, id, { successMsg: i18n.t('deleted successfully') });
      alarmReportStore.effects.getReportTasks(defaultPagingReq);
    },
    async switchReportTask({ call, select }, payload: { id: number; enable: boolean }) {
      const { pageNo, pageSize } = select((s) => s.reportTaskPaging);
      await call(switchReportTask, payload, { successMsg: i18n.t('updated successfully') });
      alarmReportStore.effects.getReportTasks({ pageNo, pageSize });
    },
    async getReportTaskRecords(
      { call, update, getParams },
      payload: Merge<IPagingReq, { start?: number; end?: number }>,
    ) {
      const { taskId } = getParams();
      const orgId = orgStore.getState((s) => s.currentOrg.id);
      const { list } = await call(
        getReportTaskRecords,
        { scope: 'org', scopeId: String(orgId), taskId, ...payload },
        { paging: { key: 'reportTaskRecordPaging' } },
      );
      update({ reportTaskRecords: list });
    },
    async getReportTaskRecord({ call, update }, id: number) {
      const reportTaskRecord = await call(getReportTaskRecord, id);
      update({ reportTaskRecord });
    },
    async getSystemDashboards({ call, update }) {
      const { list } = await call(getSystemDashboards, { scope: 'report', scopeId: 'org', pageNo: 1, pageSize: 100 });
      update({ systemDashboards: list });
    },
    async getReportTypes({ call, update }) {
      const { list: reportTypes } = await call(getReportTypes);
      update({ reportTypes });
    },
    async getAlarmReport({ call, update, getParams }, { type, query }: COMMON_ALARM_REPORT.GetAlarmReport) {
      const { clusterName } = getParams();
      const commonQuery = {
        start: 'before_20m',
        end: 'after_10m',
        filter_cluster_name: clusterName,
      };
      const { handler, chartType, extraQuery } = ALARM_REPORT_CHART_MAP[type];
      let request = getAlarmReport;
      switch (chartType) {
        case 'cpu':
          request = getCPUAlarmReport;
          break;
        case 'system':
          request = getSystemReport;
          break;
        default:
          break;
      }

      const data = await call(request, {
        query: {
          ...commonQuery,
          ...extraQuery,
          ...query,
        },
        chartType,
      });

      update({
        alarmReport: { ...handler(data), loading: false },
      });
    },
    async getProcessCmdline(
      { call, update },
      payload: Omit<COMMON_ALARM_REPORT.QueryCmdLine, 'start' | 'end' | 'group'>,
    ) {
      const commonQuery = {
        start: 'before_20m',
        end: 'after_10m',
        group: 'cmdline',
      };
      const { results } = await call(getProcessCmdline, { ...commonQuery, ...payload });
      update({
        processCmdline: results[0].data[0] ? results[0].data[0].tag : '',
      });
    },
  },
  reducers: {
    clearReportTaskRecords(state) {
      state.reportTaskRecords = [];
    },
    clearReportTaskRecord(state) {
      state.reportTaskRecord = {};
    },
  },
});

export default alarmReportStore;

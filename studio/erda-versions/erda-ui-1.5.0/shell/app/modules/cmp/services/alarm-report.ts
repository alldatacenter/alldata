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

import agent from 'agent';

export const createReportTask = (payload: Merge<COMMON_ALARM_REPORT.ReportTaskQuery, IScope>) => {
  return agent
    .post('/api/org/report/tasks')
    .send(payload)
    .then((response: any) => response.body);
};

export const getReportTasks = (payload: Merge<IPagingReq, IScope>): IPagingResp<COMMON_ALARM_REPORT.ReportTask> => {
  return agent
    .get('/api/org/report/tasks')
    .query(payload)
    .then((response: any) => response.body);
};

export const getReportTask = (id: number): COMMON_ALARM_REPORT.ReportTask => {
  return agent.get(`/api/org/report/tasks/${id}`).then((response: any) => response.body);
};

export const updateReportTask = ({ id, ...rest }: Merge<COMMON_ALARM_REPORT.ReportTaskQuery, { id: number }>) => {
  return agent
    .put(`/api/org/report/tasks/${id}`)
    .send(rest)
    .then((response: any) => response.body);
};

export const deleteReportTask = (id: number) => {
  return agent.delete(`/api/org/report/tasks/${id}`).then((response: any) => response.body);
};

export const switchReportTask = ({ id, enable }: { id: number; enable: boolean }) => {
  return agent
    .put(`/api/org/report/tasks/${id}/switch`)
    .query({ enable })
    .then((response: any) => response.body);
};

export const getReportTaskRecords = (
  payload: Merge<IPagingReq, Merge<IScope, { taskId: number; start?: number; end?: number }>>,
): IPagingResp<COMMON_ALARM_REPORT.ReportRecord> => {
  return agent
    .get('/api/report/histories')
    .query(payload)
    .then((response: any) => response.body);
};

export const getReportTaskRecord = (id: number): COMMON_ALARM_REPORT.ReportRecord => {
  return agent.get(`/api/report/histories/${id}`).then((response: any) => response.body);
};

export const getSystemDashboards = (
  payload: Merge<IPagingReq, IScope>,
): IPagingResp<COMMON_ALARM_REPORT.DashboardBlock> => {
  return agent
    .get('/api/dashboard/system/blocks')
    .query(payload)
    .then((response: any) => response.body);
};

export const getReportTypes = (): IPagingResp<COMMON_ALARM_REPORT.ReportType> => {
  return agent.get('/api/report/types').then((response: any) => response.body);
};

export const getAlarmReport = ({
  query,
  chartType,
}: {
  query: COMMON_ALARM_REPORT.AlarmReportQuerys;
  chartType: string;
}): COMMON_ALARM_REPORT.AlarmReports => {
  return agent
    .get(`/api/orgCenter/metrics/${chartType}/histogram`)
    .query(query)
    .then((response: any) => response.body);
};

export const getSystemReport = ({
  query,
}: {
  query: COMMON_ALARM_REPORT.AlarmReportQuerys;
}): COMMON_ALARM_REPORT.AlarmReports => {
  return agent
    .get('/api/orgCenter/metrics/machine_load/histogram')
    .query(query)
    .then((response: any) => response.body);
};

export const getCPUAlarmReport = ({
  query,
}: {
  query: COMMON_ALARM_REPORT.AlarmReportQuerys;
}): COMMON_ALARM_REPORT.AlarmReports => {
  return agent
    .get('/api/orgCenter/metrics/machine_cpu/histogram')
    .query(query)
    .then((response: any) => response.body);
};

export const getProcessCmdline = (payload: COMMON_ALARM_REPORT.QueryCmdLine): COMMON_ALARM_REPORT.AlarmReports => {
  return agent
    .get('/api/orgCenter/metrics/procstat')
    .query(payload)
    .then((response: any) => response.body);
};

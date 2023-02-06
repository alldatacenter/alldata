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

export const getProjectReport = ({
  projectId,
  workspace,
  type,
  pageSize,
  pageNo,
  start,
  end,
}: PROJECT_REPORT.QueryReport): IPagingResp<PROJECT_REPORT.IReport> => {
  return agent
    .get(`/api/report/${projectId}/${workspace}/${type}`)
    .query({ page: pageNo, size: pageSize, start, end })
    .then((response: any) => response.body);
};

export const getProjectReportDetail = ({ projectId, workspace, type, key }: PROJECT_REPORT.GetReportDetail): string => {
  return agent
    .get(`/api/report/${projectId}/${workspace}/${type}/render`)
    .query({ reportId: key })
    .then((response: any) => response.text);
};

export const getProjectReportSetting = ({
  projectId,
  workspace,
}: PROJECT_REPORT.QueryConfig): PROJECT_REPORT.Config => {
  return agent.get(`/api/report/${projectId}/${workspace}/settings`).then((response: any) => response.body);
};

export const setProjectReportSetting = ({ projectId, workspace, setting }: PROJECT_REPORT.SetConfig): string => {
  return agent
    .post(`/api/report/${projectId}/${workspace}/settings`)
    .send(setting)
    .then((response: any) => response.body);
};

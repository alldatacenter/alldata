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

export const getProjectDashboard = (query: MONITOR_STATUS.IDashboardQuery): MONITOR_STATUS.IDashboardResp => {
  return agent
    .get('/api/spot/msp/apm/checkers/dashboard')
    .query(query)
    .then((response: any) => response.body);
};

export const getStatusDetail = ({
  id,
  period,
}: MONITOR_STATUS.IDashboardDetailQuery): MONITOR_STATUS.IDashboardResp => {
  return agent
    .get(`/api/spot/msp/apm/checker/${id}/dashboard`)
    .query({ period })
    .then((response: any) => response.body);
};

export const getPastIncidents = ({ id }: { id: string }): MONITOR_STATUS.IPastIncidents[] => {
  return agent.get(`/api/spot/msp/apm/checker/${id}/issues`).then((response: any) => response.body);
};

export const getMetricStatus = ({ metricId }: { metricId: string }): MONITOR_STATUS.IMetrics => {
  return agent.get(`/api/spot/msp/apm/checker/${metricId}/status`).then((response: any) => response.body);
};

export const saveService = ({ data }: MONITOR_STATUS.ICreateMetricsBody) => {
  return agent
    .post('/api/spot/msp/apm/checker')
    .send(data)
    .then((response: any) => response.body);
};

export const updateMetric = (data: MONITOR_STATUS.IMetricsBody) => {
  return agent
    .post(`/api/spot/msp/apm/checker/${data.id}`)
    .send(data)
    .then((response: any) => response.body);
};

export const deleteMetric = (id: string) => {
  return agent.delete(`/api/spot/msp/apm/checker/${id}`).then((response: any) => response.body);
};

export const setDatumPoint = (data: { id: string; url: string }) => {
  return agent
    .post('/api/status/datumpoint/browser')
    .send(data)
    .then((response: any) => response.body);
};

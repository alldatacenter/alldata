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

export const createCustomDashboard = (payload: Custom_Dashboard.DashboardItem) => {
  return agent
    .post('/api/tmc/dashboard/blocks')
    .query({ scopeId: payload.scopeId })
    .send(payload)
    .then((response: any) => response.body);
};

export const updateCustomDashboard = (payload: Custom_Dashboard.DashboardItem) => {
  return agent
    .put(`/api/tmc/dashboard/blocks/${payload.id}`)
    .query({ scopeId: payload.scopeId })
    .send(payload)
    .then((response: any) => response.body);
};

export const deleteCustomDashboard = ({ id, scopeId }: { id: string; scopeId: string }) => {
  return agent
    .delete(`/api/tmc/dashboard/blocks/${id}`)
    .query({ scopeId })
    .then((response: any) => response.body);
};

export const getCustomDashboardDetail = ({
  id,
  scopeId,
}: {
  id: string;
  scopeId: string;
}): Custom_Dashboard.DashboardItem => {
  return agent
    .get(`/api/tmc/dashboard/blocks/${id}`)
    .query({ scopeId })
    .then((response: any) => response.body);
};

export const getCustomDashboard = (
  payload: Custom_Dashboard.GetDashboardPayload,
): IPagingResp<Custom_Dashboard.DashboardItem> => {
  return agent
    .get('/api/tmc/dashboard/blocks')
    .query(payload)
    .then((response: any) => response.body);
};

export const getChartData = ({ url, query }: { url: string; query?: any }) =>
  agent
    .get(url)
    .query(query)
    .then((response: any) => response.body);

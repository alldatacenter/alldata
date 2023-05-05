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

export const getProcessDashboardId = (
  query: TOPOLOGY_SERVICE_ANALYZE.CommonQuery,
): TOPOLOGY_SERVICE_ANALYZE.ProcessDashboardId => {
  return agent
    .get('/api/apm/topology/process/dashboardId')
    .query(query)
    .then((response: any) => response.body);
};

export const getTraceSlowTranslation = (
  query: Merge<
    TOPOLOGY_SERVICE_ANALYZE.CommonQuery,
    { operation: string; start: number; end: number; sort: TOPOLOGY_SERVICE_ANALYZE.SORT_TYPE; limit: number }
  >,
): TOPOLOGY_SERVICE_ANALYZE.TranslationSlowResp => {
  return agent
    .get('/api/apm/topology/translation/slow')
    .query(query)
    .then((response: any) => response.body);
};

export const getExceptionTypes = (
  query: Merge<TOPOLOGY_SERVICE_ANALYZE.CommonQuery, TOPOLOGY_SERVICE_ANALYZE.TimestampQuery>,
): { data?: string[] } => {
  return agent
    .get('/api/apm/topology/exception/types')
    .query(query)
    .then((response: any) => response.body);
};

export const getInstanceIds = (
  query: Merge<TOPOLOGY_SERVICE_ANALYZE.CommonQuery, TOPOLOGY_SERVICE_ANALYZE.TimestampQuery>,
): { data?: TOPOLOGY_SERVICE_ANALYZE.InstanceId[] } => {
  return agent
    .get('/api/apm/topology/service/instance/ids')
    .query(query)
    .then((response: any) => response.body);
};

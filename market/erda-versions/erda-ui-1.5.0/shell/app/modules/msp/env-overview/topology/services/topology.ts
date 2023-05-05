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

export const getMonitorTopology = (query: TOPOLOGY.ITopologyQuery): TOPOLOGY.ITopologyResp => {
  return agent
    .get('/api/apm/topology')
    .query({ ...query })
    .then((response: any) => response.body);
};

export const getErrorDetail = (query: TOPOLOGY.IErrorDetailQuery): IChartResult => {
  return agent
    .get('/api/tmc/metrics/application_http')
    .query(query)
    .then((response: any) => response.body);
};

export const getExceptionDetail = (query: TOPOLOGY.IExceptionQuery): IChartResult => {
  return agent
    .get('/api/tmc/metrics/error_count')
    .query(query)
    .then((response: any) => response.body);
};

export const getTopologyTags = (query: any) => {
  return agent
    .get('/api/apm/topology/search/tags')
    .query(query)
    .then((response: any) => response.body);
};

export const getTagsOptions = (query: any) => {
  return agent
    .get('/api/apm/topology/search/tagv')
    .query(query)
    .then((response: any) => response.body);
};

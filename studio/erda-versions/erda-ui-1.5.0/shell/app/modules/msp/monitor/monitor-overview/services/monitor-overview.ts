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

const reqUrlPrefix = '/api/spot/tmc/metrics';

export const getAiCapacityData = (params: MONITOR_OVERVIEW.IChartQuery): IChartResult => {
  return agent
    .get(`${reqUrlPrefix}/ai_overview`)
    .query(params)
    .then((response: any) => response.body);
};

export const getBiCapacityApdex = (params: MONITOR_OVERVIEW.IChartQuery): IChartResult => {
  return agent
    .get(`${reqUrlPrefix}/apdex`)
    .query(params)
    .then((response: any) => response.body);
};

export const getBiCapacityAjaxErr = (params: MONITOR_OVERVIEW.IChartQuery) => {
  return agent
    .get(`${reqUrlPrefix}/ta_req_status/range`)
    .query(params)
    .then((response: any) => response.body);
};

export const getBiCapacityAjaxInfo = (params: MONITOR_OVERVIEW.IChartQuery) => {
  return agent
    .get(`${reqUrlPrefix}/ta_overview`)
    .query(params)
    .then((response: any) => response.body);
};

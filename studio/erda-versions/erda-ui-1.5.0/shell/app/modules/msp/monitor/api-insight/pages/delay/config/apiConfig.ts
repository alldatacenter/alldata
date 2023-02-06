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

import { groupHandler, slowHandler } from 'common/utils/chart-utils';
import { gatewayApiPrefix } from '../../config';

const commonQuery = {};

export const ApiMap = {
  requestDelay: {
    isCustomApi: true,
    fetchApi: `${gatewayApiPrefix}/kong_req_latency/histogram`,
    query: { ...commonQuery, avg: 'reql_mean' },
    dataHandler: groupHandler(['avg.reql_mean']),
  },
  requestDelayTop: {
    isCustomApi: true,
    fetchApi: `${gatewayApiPrefix}/kong_req_latency`,
    query: { group: 'mapi', limit: 10, sort: 'avg_reql_mean', avg: 'reql_mean' },
    dataHandler: slowHandler(['time:avg.reql_mean']),
  },
  backendDelay: {
    isCustomApi: true,
    fetchApi: `${gatewayApiPrefix}/kong_upstream_latency/histogram`,
    query: { ...commonQuery, avg: 'upl_mean' },
    dataHandler: groupHandler(['avg.upl_mean']),
  },
  backendDelayTop: {
    isCustomApi: true,
    fetchApi: `${gatewayApiPrefix}/kong_upstream_latency`,
    query: { group: 'mapi', limit: 10, sort: 'avg_upl_mean', avg: 'upl_mean' },
    dataHandler: slowHandler(['time:avg.upl_mean']),
  },
};

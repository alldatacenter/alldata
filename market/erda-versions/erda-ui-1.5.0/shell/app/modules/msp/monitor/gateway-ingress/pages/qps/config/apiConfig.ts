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

import { groupHandler } from 'common/utils/chart-utils';

export const ApiMap = {
  overallQPS: {
    fetchApi: 'gateway_qps/histogram',
    query: { sumCps: 'cnt_sum' },
    dataHandler: groupHandler('sumCps.cnt_sum'),
  },
  successQPS: {
    fetchApi: 'gateway_qps/histogram',
    query: { sumCps: 'cnt_sum', group: 'http_status_code', filter_req_success: 1 },
    dataHandler: groupHandler('sumCps.cnt_sum'),
  },
  QPS4xx: {
    fetchApi: 'gateway_qps/histogram',
    query: { sumCps: 'cnt_sum', group: 'http_status_code', filter_req_client_err: 1 },
    dataHandler: groupHandler('sumCps.cnt_sum'),
  },
  QPS5xx: {
    fetchApi: 'gateway_qps/histogram',
    query: { sumCps: 'cnt_sum', group: 'http_status_code', filter_req_server_err: 1 },
    dataHandler: groupHandler('sumCps.cnt_sum'),
  },
};

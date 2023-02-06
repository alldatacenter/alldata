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
  requestSize: {
    isCustomApi: true,
    fetchApi: `${gatewayApiPrefix}/kong_req_length/histogram`,
    query: { ...commonQuery, avg: 'reqt_mean' },
    dataHandler: groupHandler(['avg.reqt_mean']),
  },
  requestSizeTop: {
    isCustomApi: true,
    fetchApi: `${gatewayApiPrefix}/kong_req_length`,
    query: { group: 'mapi', limit: 10, sort: 'avg_reqt_mean', avg: 'reqt_mean' },
    dataHandler: slowHandler(['size:avg.reqt_mean']),
  },
  responseSize: {
    isCustomApi: true,
    fetchApi: `${gatewayApiPrefix}/kong_resp_length/histogram`,
    query: { ...commonQuery, avg: 'bst_mean' },
    dataHandler: groupHandler(['avg.bst_mean']),
  },
  responseSizeTop: {
    isCustomApi: true,
    fetchApi: `${gatewayApiPrefix}/kong_resp_length`,
    query: { group: 'mapi', limit: 10, sort: 'avg_bst_mean', avg: 'bst_mean' },
    dataHandler: slowHandler(['size:avg.bst_mean']),
  },
};

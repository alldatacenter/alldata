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

import { multipleDataHandler } from 'common/utils/chart-utils';

const commonQuery = {};
export const ApiMap = {
  performanceInterval: {
    fetchApi: 'ta_m_performance/histogram',
    query: { ...commonQuery, avg: 'plt' },
    dataHandler: multipleDataHandler(['avg.plt']),
  },
  pagePerformanceTrends: {
    fetchApi: 'ta_m_page_timing/histogram',
    query: { ...commonQuery, count: 'plt', avg: 'plt' },
    dataHandler: multipleDataHandler(['avg.plt', 'count.plt']),
  },
  pageError: {
    fetchApi: 'ta_m_page_error/histogram',
    query: { ...commonQuery, group: 'error', limit: 3, sort: 'count', count: 'count', source: true },
    dataHandler: multipleDataHandler(['count.count']),
  },
  reqPerformanceTrends: {
    fetchApi: 'ta_m_req_timing/histogram',
    query: { ...commonQuery, count: 'tt', avg: 'tt' },
    dataHandler: multipleDataHandler(['avg.tt', 'count.tt']),
  },
};

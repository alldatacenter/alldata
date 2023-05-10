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

import { sortHandler, multipleDataHandler, groupHandler } from 'common/utils/chart-utils';

const commonQuery = {};
export const ApiMap = {
  sortList: {
    getFetchObj: ({ sortTab }: { sortTab: string }) => {
      const fetchMap = {
        time: {
          fetchApi: 'ta_m_req_top_avg_time',
          query: { group: 'req_path', limit: 20, sort: 'avg_tt', avg: 'tt' },
          dataKey: 'avg.tt',
        },
        percent: {
          fetchApi: 'ta_m_req_top_percent_time',
          query: { group: 'req_path', limit: 20, sort: 'sumPercent_tt', sumPercent: 'tt' },
          dataKey: 'sumPercent.tt',
        },
        cpm: {
          fetchApi: 'ta_m_req_top_cpm',
          query: { group: 'req_path', limit: 20, sort: 'cpm_tt', cpm: 'tt' },
          dataKey: 'cpm.tt',
        },
      };
      const { query = {}, fetchApi = '', dataKey = '' } = fetchMap[sortTab] || {};
      return { fetchApi, extendQuery: { ...query }, extendHandler: { dataKey } };
    },
    dataHandler: sortHandler(),
  },
  rspTopN: {
    fetchApi: 'ta_m_req_avg_time/histogram',
    query: { ...commonQuery, group: 'req_path', limit: 5, sort: 'histogram_avg_tt', avg: 'tt' },
    dataHandler: groupHandler('avg.tt'),
  },
  cpmTopN: {
    fetchApi: 'ta_m_req_cpm/histogram',
    query: { ...commonQuery, group: 'req_path', limit: 5, sort: 'histogram_cpm_tt', cpm: 'tt' },
    dataHandler: groupHandler('cpm.tt'),
  },
  postTopN: {
    fetchApi: 'ta_m_req_send/histogram',
    query: { ...commonQuery, group: 'req_path', limit: 5, sort: 'histogram_sum_res', sum: 'res' },
    dataHandler: groupHandler('sum.res'),
  },
  receiveTopN: {
    fetchApi: 'ta_m_req_recv/histogram',
    query: { ...commonQuery, group: 'req_path', limit: 5, sort: 'histogram_sum_req', sum: 'req' },
    dataHandler: groupHandler('sum.req'),
  },
  ajaxPerformanceTrends: {
    fetchApi: 'ta_m_req_timing/histogram',
    query: { ...commonQuery, count: 'tt', avg: 'tt' },
    dataHandler: multipleDataHandler(['avg.tt', 'count.tt']),
  },
  statusTopN: {
    fetchApi: 'ta_m_req_status/histogram',
    query: { ...commonQuery, group: 'status_code', limit: 5, sort: 'histogram_count_req', count: 'req' },
    dataHandler: groupHandler('count.req'),
  },
};

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
          fetchApi: 'ta_m_top_avg_time',
          query: { group: 'device', limit: 20, sort: 'avg_plt', avg: 'plt' },
          dataKey: 'avg.plt',
        },
        percent: {
          fetchApi: 'ta_m_top_percent_time',
          query: { group: 'device', limit: 20, sort: 'sumPercent_plt', sumPercent: 'plt' },
          dataKey: 'sumPercent.plt',
        },
        cpm: {
          fetchApi: 'ta_m_top_cpm',
          query: { group: 'device', limit: 20, sort: 'cpm_plt', cpm: 'plt' },
          dataKey: 'cpm.plt',
        },
      };
      const { query = {}, fetchApi = '', dataKey = '' } = fetchMap[sortTab] || {};
      return { fetchApi, extendQuery: { ...query }, extendHandler: { dataKey } };
    },
    dataHandler: sortHandler(),
  },
  timeTopN: {
    fetchApi: 'ta_m_avg_time/histogram',
    query: { ...commonQuery, group: 'device', limit: 5, sort: 'histogram_avg_plt', avg: 'plt' },
    dataHandler: groupHandler('avg.plt'),
  },
  cpmTopN: {
    fetchApi: 'ta_m_cpm/histogram',
    query: { ...commonQuery, group: 'device', limit: 5, sort: 'histogram_cpm_plt', cpm: 'plt' },
    dataHandler: groupHandler('cpm.plt'),
  },
  browserPerformanceInterval: {
    fetchApi: 'ta_m_performance/histogram',
    query: { ...commonQuery, avg: ['plt'] },
    dataHandler: multipleDataHandler(['avg.plt']),
  },
  singleTimeTopN: {
    fetchApi: 'ta_m_avg_time/histogram',
    query: { ...commonQuery, avg: 'plt' },
    dataHandler: groupHandler('avg.plt'),
  },
  singleCpmTopN: {
    fetchApi: 'ta_m_cpm/histogram',
    query: { ...commonQuery, cpm: 'plt' },
    dataHandler: groupHandler('cpm.plt'),
  },
};

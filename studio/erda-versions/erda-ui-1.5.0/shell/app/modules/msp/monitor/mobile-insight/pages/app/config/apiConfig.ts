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

import { sortHandler, multipleDataHandler, groupHandler, slowHandler } from 'common/utils/chart-utils';

const commonQuery = {};
export const ApiMap = {
  sortList: {
    getFetchObj: ({ sortTab }: { sortTab: string }) => {
      const fetchMap = {
        time: {
          fetchApi: 'ta_m_top_avg_time',
          query: { group: 'av', avg: 'plt', limit: 20, sort: 'avg_plt' },
          dataKey: 'avg.plt',
        },
        percent: {
          fetchApi: 'ta_m_top_percent_time',
          query: { group: 'av', sumPercent: 'plt', limit: 20, sort: 'sumPercent_plt' },
          dataKey: 'sumPercent.plt',
        },
        cpm: {
          fetchApi: 'ta_m_top_cpm',
          query: { group: 'av', cpm: 'plt', limit: 20, sort: 'cpm_plt' },
          dataKey: 'cpm.plt',
        },
      };
      const { query = {}, fetchApi = '', dataKey = '' } = fetchMap[sortTab] || {};
      return { fetchApi, extendQuery: { ...query }, extendHandler: { dataKey } };
    },
    dataHandler: sortHandler(),
  },
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
  timeTopN: {
    fetchApi: 'ta_m_avg_time/histogram',
    query: { ...commonQuery, group: 'av', limit: 5, sort: 'histogram_avg_plt', avg: 'plt' },
    dataHandler: groupHandler('avg.plt'),
  },
  cpmTopN: {
    fetchApi: 'ta_m_cpm/histogram',
    query: { ...commonQuery, group: 'av', limit: 5, sort: 'histogram_cpm_plt', cpm: 'plt' },
    dataHandler: groupHandler('cpm.plt'),
  },
  pageTopN: {
    fetchApi: 'ta_m_timing',
    query: {
      group: 'doc_path',
      limit: 10,
      sort: 'count_plt',
      count: 'plt',
      max: 'plt',
      min: 'plt',
      maxFieldTimestamp: 'plt',
    },
    dataHandler: slowHandler(['max:max.plt', 'count:count.plt', 'time:maxFieldTimestamp.plt', 'min:min.plt']),
  },
  slowTrack: {
    fetchApi: 'ta_m_timing_slow',
    query: {
      group: 'doc_path',
      limit: 10,
      sort: 'max_plt',
      max: 'plt',
      min: 'plt',
      maxFieldTimestamp: 'plt',
      source: true,
    },
    dataHandler: slowHandler(['max:max.plt', 'time:maxFieldTimestamp.plt', 'min:min.plt']),
  },
};

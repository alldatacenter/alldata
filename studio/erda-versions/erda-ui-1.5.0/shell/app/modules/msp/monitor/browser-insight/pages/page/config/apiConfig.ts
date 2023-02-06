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
    getFetchObj: ({ sortTab, subTab }: { sortTab: string; subTab: string }) => {
      const fetchMap = {
        time: {
          fetchApi: 'ta_top_avg_time',
          query: { group: 'doc_path', avg: subTab || 'plt', limit: 20, sort: `avg_${subTab || 'plt'}` },
          dataKey: `avg.${subTab || 'plt'}`,
        },
        percent: {
          fetchApi: 'ta_top_percent_time',
          query: { group: 'doc_path', sumPercent: subTab || 'plt', limit: 20, sort: `sumPercent_${subTab || 'plt'}` },
          dataKey: `sumPercent.${subTab || 'plt'}`,
        },
        cpm: {
          fetchApi: 'ta_top_cpm',
          query: { group: 'doc_path', cpm: subTab || 'plt', limit: 20, sort: `cpm_${subTab || 'plt'}` },
          dataKey: `cpm.${subTab || 'plt'}`,
        },
      };
      const { query = {}, fetchApi = '', dataKey = '' } = fetchMap[sortTab] || {};
      return { fetchApi, extendQuery: { ...query }, extendHandler: { dataKey } };
    },
    dataHandler: sortHandler(),
  },
  performanceInterval: {
    fetchApi: 'ta_performance/histogram',
    query: { ...commonQuery, avg: ['tcp', 'srt', 'plt', 'rlt'] },
    dataHandler: multipleDataHandler(['avg.plt', 'avg.rlt', 'avg.srt', 'avg.tcp']),
  },
  timeTopN: {
    fetchApi: 'ta_avg_time/histogram',
    query: { ...commonQuery, group: 'doc_path', limit: 5, sort: 'histogram_avg_plt', avg: 'plt' },
    dataHandler: groupHandler('avg.plt'),
  },
  cpmTopN: {
    fetchApi: 'ta_cpm/histogram',
    query: { ...commonQuery, group: 'doc_path', limit: 5, sort: 'histogram_cpm_plt', cpm: 'plt' },
    dataHandler: groupHandler('cpm.plt'),
  },
  pagePerformanceTrends: {
    fetchApi: 'ta_page_timing/histogram',
    query: { ...commonQuery, count: 'plt', avg: 'plt' },
    dataHandler: multipleDataHandler(['avg.plt', 'count.plt']),
  },
  pageTopN: {
    fetchApi: 'ta_page_timing/histogram',
    query: { count: 'plt', avg: 'plt' },
    dataHandler: slowHandler(['max:max.plt', 'count:count.plt', 'time:maxFieldTimestamp.plt', 'min:min.plt']),
  },
  slowTrack: {
    fetchApi: 'ta_timing_slow',
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

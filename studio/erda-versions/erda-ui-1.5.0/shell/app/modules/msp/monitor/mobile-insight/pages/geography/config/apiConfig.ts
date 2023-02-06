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

import { endsWith, map, get } from 'lodash';
import { sortHandler, multipleDataHandler, slowHandler } from 'common/utils/chart-utils';
import i18n from 'i18n';

const commonQuery = {};
export const ApiMap = {
  sortList: {
    getFetchObj: ({ sortTab }: { sortTab: string }) => {
      const fetchMap = {
        time: {
          fetchApi: 'ta_m_top_avg_time',
          query: { group: 'province', limit: 20, sort: 'avg_plt', avg: 'plt' },
          dataKey: 'avg.plt',
        },
        percent: {
          fetchApi: 'ta_m_top_percent_time',
          query: { group: 'province', limit: 20, sort: 'sumPercent_plt', sumPercent: 'plt' },
          dataKey: 'sumPercent.plt',
        },
        cpm: {
          fetchApi: 'ta_m_top_cpm',
          query: { group: 'province', limit: 20, sort: 'cpm_plt', cpm: 'plt' },
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
    query: { ...commonQuery, avg: ['plt'] },
    dataHandler: multipleDataHandler(['avg.plt']),
  },
  pagePerformanceTrends: {
    fetchApi: 'ta_m_page_timing/histogram',
    query: { ...commonQuery, count: 'plt', avg: 'plt' },
    dataHandler: multipleDataHandler(['avg.plt', 'count.plt']),
  },
  regionalLoadingTime: {
    fetchApi: 'ta_m_map',
    query: { limit: 60, group: 'province', avg: 'plt', cpm: 'plt' },
    dataHandler: (orignData: object) => {
      const reData: any = {
        results: [],
        pieces: [
          { lt: 1, label: '<1s' },
          { gte: 1, lt: 2, label: '1 ~ 2s' },
          { gte: 2, lt: 3, label: '2 ~ 3s' },
          { gte: 3, lt: 4, label: '3 ~ 4s' },
          { gte: 4, lt: 5, label: '4 ~ 5s' },
          { gte: 5, lt: 6, label: '5 ~ 6s' },
          { gte: 6, lt: 7, label: '6 ~ 7s' },
          { gte: 7, label: '>= 7s' },
        ],
      };
      const list = get(orignData, 'results[0].data');
      if (list.length > 0) {
        const arr = map(list, (item) => {
          const { tag = '', name = '', data = 0 } = item['avg.plt'] || {};
          const cpm = item['cpm.plt'] || {};
          let _name = tag || name;
          if (endsWith(_name, '省') || endsWith(_name, '市')) {
            _name = _name.slice(0, _name.length - 1);
          }
          return { name: _name, value: data / 1000, tps: cpm.data || 0 };
        });
        reData.results = [{ data: arr, type: i18n.t('msp:average load time') }];
      }
      return { ...reData };
    },
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

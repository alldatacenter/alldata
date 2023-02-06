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

import { isArray, map, get } from 'lodash';
import moment from 'moment';
import { multipleDataHandler, groupHandler } from 'common/utils/chart-utils';

const commonQuery = {};
export const ApiMap = {
  performanceInterval: {
    fetchApi: 'ta_performance/histogram',
    query: { ...commonQuery, avg: ['tcp', 'srt', 'plt', 'rlt'] },
    dataHandler: multipleDataHandler(['avg.plt', 'avg.rlt', 'avg.srt', 'avg.tcp']),
  },
  pagePerformanceTrends: {
    fetchApi: 'ta_page_timing/histogram',
    query: { ...commonQuery, count: 'plt', avg: 'plt' },
    dataHandler: multipleDataHandler(['avg.plt', 'count.plt']),
  },
  scriptError: {
    fetchApi: 'ta_script_error/histogram',
    query: { ...commonQuery, group: 'error', limit: 3, sort: 'count', count: 'count', source: true },
    dataHandler: groupHandler('count.count'),
  },
  ajaxPerformanceTrends: {
    fetchApi: 'ta_ajax_timing/histogram',
    query: { ...commonQuery, count: 'tt', avg: 'tt' },
    dataHandler: multipleDataHandler(['avg.tt', 'count.tt']),
  },
  loadPVUV: {
    fetchApi: 'ta_overview_days/histogram',
    query: { count: 'plt', points: 7, align: false },
    dataHandler: (originData = {}) => {
      const { time, results } = originData as any;
      const res = get(results, '[0].data[0]["count.plt"]');
      return {
        results: [{ ...res }],
        xAxis: isArray(time) && map(time, (_time) => moment(Number(_time)).format('MM-DD')),
      };
    },
  },
};

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

import { sortHandler } from 'common/utils/chart-utils';
import { get } from 'lodash';

export const ApiMap = {
  sortList: {
    fetchApi: 'ta_top_avg_time',
    getFetchObj: ({ sortTab }: { sortTab: string }) => {
      const commonQ = { limit: 20, sort: 'avg_plt', avg: 'plt' };
      const fetchMap = {
        host: { query: { group: 'host', ...commonQ }, dataKey: 'avg.plt' },
        url: { query: { group: 'doc_path', ...commonQ }, dataKey: 'avg.plt' },
        os: { query: { group: 'os', ...commonQ }, dataKey: 'avg.plt' },
        browser: { query: { group: 'browser', ...commonQ }, dataKey: 'avg.plt' },
        device: { query: { group: 'device', ...commonQ }, dataKey: 'avg.plt' },
      };
      const { query = {}, dataKey = '' } = fetchMap[sortTab] || {};
      return { extendQuery: { ...query }, extendHandler: { dataKey } };
    },
    dataHandler: sortHandler(),
  },
  summaryDetail: {
    fetchApi: 'ta_summary_info',
    query: {
      avg: ['rrt', 'put', 'act', 'dns', 'tcp', 'rpt', 'srt', 'dit', 'drt', 'clt', 'plt', 'wst', 'set', 'net'],
      sum: 'rdc',
      count: 'plt',
    },
    dataHandler: (originData = {}) => {
      const detail = get(originData, 'results[0].data[0]');
      return { data: detail };
    },
  },
};

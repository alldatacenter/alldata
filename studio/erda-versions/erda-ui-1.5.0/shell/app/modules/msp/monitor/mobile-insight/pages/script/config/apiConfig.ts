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

import { get } from 'lodash';
import { sortHandler, groupHandler } from 'common/utils/chart-utils';

const commonQuery = {};
export const ApiMap = {
  sortList: {
    getFetchObj: ({ sortTab }: { sortTab: string }) => {
      const fetchMap = {
        error: {
          fetchApi: 'ta_m_top_error',
          query: { group: 'error', count: 'count', limit: 20, sort: 'count_count', source: true },
          dataKey: 'count.count',
        },
      };
      const { fetchApi = '', query = {}, dataKey = '' } = fetchMap[sortTab] || {};
      return { fetchApi, extendQuery: query, extendHandler: { dataKey } };
    },
    dataHandler: sortHandler(),
  },
  errorTopN: {
    fetchApi: 'ta_m_top_error_count/histogram',
    query: { ...commonQuery, group: 'error', limit: 5, sort: 'histogram_count_count', count: 'count', source: true },
    dataHandler: groupHandler('count.count'),
  },
  osTopN: {
    fetchApi: 'ta_m_top_os_error/histogram',
    query: { ...commonQuery, group: 'os', limit: 5, sort: 'histogram_count_count', count: 'count', source: true },
    dataHandler: groupHandler('count.count'),
  },
  scriptDetail: {
    fetchApi: 'error_info',
    dataHandler: (originData = {}) => {
      const list = get(originData, 'results[0].data');
      return { list };
    },
  },
};

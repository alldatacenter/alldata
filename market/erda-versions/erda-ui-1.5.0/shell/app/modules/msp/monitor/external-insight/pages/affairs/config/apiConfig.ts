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

import { groupHandler, sortHandler, slowHandler, errorHttpHandler } from 'common/utils/chart-utils';

export const ApiMap = {
  sortList: {
    getFetchObj: ({ sortTab }: { sortTab: string }) => {
      const fetchMap = {
        rt: {
          fetchApi: 'ai_web_top_time',
          query: { group: 'source_service_name', avg: 'elapsed_mean', limit: 10, sort: 'avg_elapsed_mean' },
          dataKey: 'avg.elapsed_mean',
        },
        throughput: {
          fetchApi: 'ai_web_top_cpm',
          query: { group: 'source_service_name', limit: 10, sort: 'sumCpm_elapsed_count', sumCpm: 'elapsed_count' },
          dataKey: 'sumCpm.elapsed_count',
        },
      };
      const { query = {}, fetchApi = '', dataKey = '' } = fetchMap[sortTab] || {};
      return { fetchApi, extendQuery: { ...query }, extendHandler: { dataKey } };
    },
    dataHandler: sortHandler(),
  },
  responseTimes: {
    fetchApi: 'ai_web_avg_time_top5/histogram',
    query: { group: 'source_service_name', avg: 'elapsed_mean', sort: 'histogram_avg_elapsed_mean', limit: 5 },
    dataHandler: groupHandler('avg.elapsed_mean'),
  },
  throughput: {
    fetchApi: 'ai_web_cpm_top5/histogram',
    query: { group: 'source_service_name', sumCpm: 'elapsed_count', sort: 'histogram_sumCpm_elapsed_count', limit: 5 },
    dataHandler: groupHandler('sumCpm.elapsed_count'),
  },
  httpError: {
    fetchApi: 'ai_web_error_req/histogram',
    query: { group: 'http_status_code', sum: 'elapsed_count', filter_error: true },
    dataHandler: groupHandler('sum.elapsed_count'),
  },
  slowTrack: {
    fetchApi: 'application_http_slow',
    query: {
      group: 'source_service_name',
      limit: 10,
      sort: 'max_elapsed_max',
      max: 'elapsed_max',
      min: 'elapsed_min',
      maxFieldTimestamp: 'elapsed_max',
      source: true,
      sum: 'elapsed_count',
      last: 'tags.source_application_id',
    },
    dataHandler: slowHandler([
      'max:max.elapsed_max',
      'count:sum.elapsed_count',
      'time:maxFieldTimestamp.elapsed_max',
      'min:min.elapsed_min',
      'applicationId:last.tags.source_application_id',
    ]),
  },
  errorTrack: {
    fetchApi: 'application_http_error',
    query: {
      group: ['source_service_name', 'http_status_code'],
      limit: 10,
      field_gte_http_status_code_min: 500,
      sum: 'elapsed_count',
      sort: 'sum_elapsed_count',
      maxFieldTimestamp: 'elapsed_max',
      last: 'tags.source_application_id',
    },
    dataHandler: errorHttpHandler(),
  },
};

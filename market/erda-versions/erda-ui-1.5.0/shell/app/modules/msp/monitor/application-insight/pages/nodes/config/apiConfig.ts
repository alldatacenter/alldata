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

import { multipleDataHandler, groupHandler } from 'common/utils/chart-utils';

const commonQ = {};
export const ApiMap = {
  heapMemoryUsage: {
    fetchApi: 'ai_nodejs_men_heap/histogram',
    query: { ...commonQ, avg: ['heap_total', 'heap_used'] },
    dataHandler: multipleDataHandler(['avg.heap_total', 'avg.heap_used']),
  },
  nonHeapMemoryUsage: {
    fetchApi: 'ai_nodejs_men_non_heap/histogram',
    query: { ...commonQ, avg: ['external', 'rss'] },
    dataHandler: multipleDataHandler(['avg.external', 'avg.rss']),
  },
  clusterCount: {
    fetchApi: 'ai_nodejs_cluster_count/histogram',
    query: { ...commonQ, max: 'count' },
    dataHandler: multipleDataHandler(['max.count']),
  },
  asyncResources: {
    fetchApi: 'ai_nodejs_async_res/histogram',
    query: { ...commonQ, sum: 'count', group: 'type', limit: 10 },
    dataHandler: groupHandler('sum.count'),
  },
};

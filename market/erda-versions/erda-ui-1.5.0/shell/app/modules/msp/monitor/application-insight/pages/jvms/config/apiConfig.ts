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
    fetchApi: 'ai_jvm_mem_heap/histogram',
    query: { ...commonQ, avg: ['init', 'max', 'used', 'committed'], filter_name: 'heap_memory' },
    dataHandler: multipleDataHandler(['avg.max', 'avg.init', 'avg.used', 'avg.committed']),
  },
  nonHeapMemoryUsage: {
    fetchApi: 'ai_jvm_mem_non_heap/histogram',
    query: { ...commonQ, avg: ['init', 'used', 'committed'], filter_name: 'non_heap_memory' },
    dataHandler: multipleDataHandler(['avg.init', 'avg.used', 'avg.committed']),
  },
  PSEdenSpace: {
    fetchApi: 'ai_jvm_mem_eden_space/histogram',
    query: { ...commonQ, avg: ['max', 'used', 'committed'], match_name: '*eden_space' },
    dataHandler: multipleDataHandler(['avg.max', 'avg.used', 'avg.committed']),
  },
  PSOldGen: {
    fetchApi: 'ai_jvm_mem_old_gen/histogram',
    query: { ...commonQ, avg: ['max', 'used', 'committed'], match_name: '*old_gen' },
    dataHandler: multipleDataHandler(['avg.max', 'avg.used', 'avg.committed']),
  },
  PSSurvivorSpace: {
    fetchApi: 'ai_jvm_mem_survivor_space/histogram',
    query: { ...commonQ, avg: ['max', 'used', 'committed'], match_name: '*survivor_space' },
    dataHandler: multipleDataHandler(['avg.max', 'avg.used', 'avg.committed']),
  },
  GCMarkSweep: {
    fetchApi: 'ai_gc_mark_sweep/histogram',
    query: { ...commonQ, sum: 'count', group: 'name' },
    dataHandler: groupHandler('sum.count'),
  },
  GCScavenge: {
    fetchApi: 'ai_gc_scavenge/histogram',
    query: { ...commonQ, avg: 'time', group: 'name' },
    dataHandler: groupHandler('avg.time'),
  },
  classCount: {
    fetchApi: 'ai_class_count/histogram',
    query: { ...commonQ, avg: ['loaded', 'unloaded'] },
    dataHandler: multipleDataHandler(['avg.loaded', 'avg.unloaded']),
  },
  thread: {
    fetchApi: 'ai_thread/histogram',
    query: { ...commonQ, avg: 'state', sort: 'histogram_avg_state', limit: 10, group: 'name' },
    dataHandler: groupHandler('avg.state'),
  },
};

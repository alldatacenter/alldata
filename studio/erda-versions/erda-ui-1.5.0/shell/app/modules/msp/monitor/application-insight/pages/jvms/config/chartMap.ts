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

import { merge } from 'lodash';
import { chartRender } from 'application-insight/common/components/aiRenderFactory';
import { ApiMap } from './apiConfig';
import i18n from 'i18n';

export const commonAttr = {
  moduleName: 'AIJvms',
};
const chartMap = merge(
  {
    heapMemoryUsage: {
      titleText: 'Heap memory usage',
      ...commonAttr,
      chartName: 'heapMemoryUsage',
      viewProps: {
        unitType: 'CAPACITY',
      },
    },
    nonHeapMemoryUsage: {
      titleText: 'Non Heap memory usage',
      ...commonAttr,
      chartName: 'nonHeapMemoryUsage',
      viewProps: {
        unitType: 'CAPACITY',
      },
    },
    PSEdenSpace: {
      titleText: 'Eden-Space',
      ...commonAttr,
      chartName: 'PSEdenSpace',
      viewProps: {
        unitType: 'CAPACITY',
      },
    },
    PSOldGen: {
      titleText: 'Old-Gen',
      ...commonAttr,
      chartName: 'PSOldGen',
      viewProps: {
        unitType: 'CAPACITY',
        gridBottom: 100,
      },
    },
    PSSurvivorSpace: {
      titleText: 'Survivor-Space',
      ...commonAttr,
      chartName: 'PSSurvivorSpace',
      viewProps: {
        unitType: 'CAPACITY',
      },
    },
    GCMarkSweep: {
      titleText: `GC ${i18n.t('times')}`,
      ...commonAttr,
      chartName: 'GCMarkSweep',
    },
    GCScavenge: {
      titleText: `GC ${i18n.t('msp:average time')}`,
      ...commonAttr,
      chartName: 'GCScavenge',
      viewProps: {
        unitType: 'TIME',
        isTwoYAxis: true,
      },
    },
    classCount: {
      titleText: 'Class count',
      ...commonAttr,
      chartName: 'ClassCount',
      viewProps: {
        isTwoYAxis: true,
      },
    },
    thread: {
      titleText: 'Thread',
      ...commonAttr,
      chartName: 'Thread',
    },
  },
  ApiMap,
);

export default {
  heapMemoryUsage: chartRender(chartMap.heapMemoryUsage) as any,
  nonHeapMemoryUsage: chartRender(chartMap.nonHeapMemoryUsage) as any,
  PSEdenSpace: chartRender(chartMap.PSEdenSpace) as any,
  PSOldGen: chartRender(chartMap.PSOldGen) as any,
  PSSurvivorSpace: chartRender(chartMap.PSSurvivorSpace) as any,
  GCMarkSweep: chartRender(chartMap.GCMarkSweep) as any,
  GCScavenge: chartRender(chartMap.GCScavenge) as any,
  classCount: chartRender(chartMap.classCount) as any,
  thread: chartRender(chartMap.thread) as any,
};

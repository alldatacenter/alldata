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

export const commonAttr = {
  moduleName: 'AINodes',
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
    clusterCount: {
      titleText: 'Cluster Count',
      ...commonAttr,
      chartName: 'clusterCount',
    },
    asyncResources: {
      titleText: 'AsyncResources',
      ...commonAttr,
      chartName: 'asyncResources',
    },
  },
  ApiMap,
);

export default {
  heapMemoryUsage: chartRender(chartMap.heapMemoryUsage) as any,
  nonHeapMemoryUsage: chartRender(chartMap.nonHeapMemoryUsage) as any,
  clusterCount: chartRender(chartMap.clusterCount) as any,
  asyncResources: chartRender(chartMap.asyncResources) as any,
};

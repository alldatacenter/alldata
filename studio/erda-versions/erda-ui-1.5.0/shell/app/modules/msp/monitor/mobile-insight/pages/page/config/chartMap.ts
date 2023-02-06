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
import { sortCreator } from 'mobile-insight/common/utils';
import { sortRender, chartRender } from 'mobile-insight/common/components/miRenderFactory';
import SlowTrace from 'mobile-insight/common/components/slow-track-panel';
import { ApiMap } from './apiConfig';
import i18n from 'i18n';

export const commonAttr = {
  moduleName: 'MIPage',
  groupId: 'miPage',
};

const chartMap = merge(
  {
    sortTab: sortCreator(commonAttr.moduleName, 'sortTab'),
    sortList: {
      ...commonAttr,
      type: 'sortList',
      chartName: 'sortList',
    },
    performanceInterval: {
      ...commonAttr,
      titleText: i18n.t('performance interval'),
      chartName: 'performanceInterval',
    },
    timeTopN: {
      titleText: i18n.t('msp:average time'),
      ...commonAttr,
      chartName: 'timeTopN',
      viewProps: {
        unitType: 'TIME',
      },
    },
    cpmTopN: {
      titleText: i18n.t('msp:throughput'),
      ...commonAttr,
      chartName: 'cpmTopN',
      viewProps: {
        unitType: 'CPM',
      },
    },
    pagePerformanceTrends: {
      ...commonAttr,
      titleText: i18n.t('msp:visit page performance trends'),
      chartName: 'performanceTrends',
    },
    slowTrack: {
      titleText: i18n.t('msp:slow loading tracking'),
      ...commonAttr,
      chartName: 'slow',
      viewRender: SlowTrace,
    },
  },
  ApiMap,
);

export default {
  sortTab: sortRender(chartMap.sortTab) as any,
  sortList: sortRender(chartMap.sortList) as any,
  performanceInterval: chartRender(chartMap.performanceInterval) as any,
  timeTopN: chartRender(chartMap.timeTopN) as any,
  cpmTopN: chartRender(chartMap.cpmTopN) as any,
  pagePerformanceTrends: chartRender(chartMap.pagePerformanceTrends) as any,
  slowTrack: chartRender(chartMap.slowTrack) as any,
};

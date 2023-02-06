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
import { sortCreator } from 'browser-insight/common/utils';
import { sortRender, chartRender } from 'browser-insight/common/components/biRenderFactory';
import TablePanel from 'browser-insight/common/components/table-panel';
import SlowTrace from 'browser-insight/common/components/slow-track-panel';
import { ApiMap } from './apiConfig';
import i18n from 'i18n';

export const commonAttr = {
  moduleName: 'BIDomain',
  groupId: 'BIDomain',
};
const chartMap = merge(
  {
    sortTab: sortCreator(commonAttr.moduleName, 'sortTab'),
    subTab: sortCreator(commonAttr.moduleName, 'subTab'),
    sortList: {
      ...commonAttr,
      type: 'sortList',
      chartName: 'sortList',
    },
    performanceInterval: {
      ...commonAttr,
      titleText: i18n.t('performance interval'),
      chartName: 'performanceInterval',
      viewProps: { unitType: 'TIME' },
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
    domainPerformanceTrends: {
      ...commonAttr,
      titleText: i18n.t('msp:access domain performance trends'),
      chartName: 'performanceTrends',
      viewProps: {
        isTwoYAxis: true,
        rightUnitType: 'TIME',
      },
    },
    pageTopN: {
      titleText: `${i18n.t('msp:visit times')}TOP10`,
      ...commonAttr,
      chartName: 'pageTopN',
      viewRender: TablePanel,
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
  subTab: sortRender(chartMap.subTab) as any,
  sortTab: sortRender(chartMap.sortTab) as any,
  sortList: sortRender(chartMap.sortList) as any,
  performanceInterval: chartRender(chartMap.performanceInterval) as any,
  timeTopN: chartRender(chartMap.timeTopN) as any,
  cpmTopN: chartRender(chartMap.cpmTopN) as any,
  domainPerformanceTrends: chartRender(chartMap.domainPerformanceTrends) as any,
  pageTopN: chartRender(chartMap.pageTopN) as any,
  slowTrack: chartRender(chartMap.slowTrack) as any,
};

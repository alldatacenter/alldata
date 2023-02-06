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
import { ApiMap } from './apiConfig';
import i18n from 'i18n';

export const commonAttr = {
  moduleName: 'BIBrowser',
  groupId: 'BIBrowser',
};

const chartMap = merge(
  {
    sortTab: sortCreator(commonAttr.moduleName, 'sortTab'),
    sortList: {
      ...commonAttr,
      type: 'sortList',
      chartName: 'sortList',
    },
    timeTopN: {
      titleText: i18n.t('msp:average load time - browser type'),
      ...commonAttr,
      chartName: 'timeTopN',
      viewProps: {
        unitType: 'TIME',
      },
    },
    cpmTopN: {
      titleText: i18n.t('msp:throughput - browser type'),
      ...commonAttr,
      chartName: 'cpmTopN',
      viewProps: {
        unitType: 'CPM',
      },
    },
    browserPerformanceInterval: {
      ...commonAttr,
      chartName: 'performanceInterval',
      titleText: i18n.t('msp:browser performance trends'),
    },
    singleTimeTopN: {
      titleText: i18n.t('msp:average load time'),
      ...commonAttr,
      chartName: 'singleTimeTopN',
      viewProps: {
        unitType: 'TIME',
      },
    },
    singleCpmTopN: {
      titleText: i18n.t('msp:throughput'),
      ...commonAttr,
      chartName: 'singleCpmTopN',
      viewProps: {
        unitType: 'CPM',
      },
    },
  },
  ApiMap,
);

export default {
  sortTab: sortRender(chartMap.sortTab) as any,
  sortList: sortRender(chartMap.sortList) as any,
  timeTopN: chartRender(chartMap.timeTopN) as any,
  cpmTopN: chartRender(chartMap.cpmTopN) as any,
  browserPerformanceInterval: chartRender(chartMap.browserPerformanceInterval) as any,
  singleTimeTopN: chartRender(chartMap.singleTimeTopN) as any,
  singleCpmTopN: chartRender(chartMap.singleCpmTopN) as any,
};

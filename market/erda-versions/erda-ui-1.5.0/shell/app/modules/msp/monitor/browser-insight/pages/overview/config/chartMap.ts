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
import { chartRender } from 'browser-insight/common/components/biRenderFactory';
import { ApiMap } from './apiConfig';
import i18n from 'i18n';

export const commonAttr = {
  moduleName: 'BIOverview',
  groupId: 'biOverview',
};
const chartMap = merge(
  {
    performanceInterval: {
      ...commonAttr,
      titleText: i18n.t('performance interval'),
      chartName: 'performanceInterval',
      viewProps: { unitType: 'TIME' },
    },
    scriptError: {
      titleText: i18n.t('msp:script error'),
      ...commonAttr,
      chartName: 'scriptError',
    },
    pagePerformanceTrends: {
      ...commonAttr,
      titleText: i18n.t('msp:page performance trends'),
      groupId: '',
      chartName: 'performanceTrends',
    },
    ajaxPerformanceTrends: {
      ...commonAttr,
      chartName: 'ajaxPerformanceTrends',
      titleText: i18n.t('msp:ajax performance trends'),
    },
    loadPVUV: {
      moduleName: 'BIOverview',
      chartName: 'loadPVUV',
      viewProps: {
        seriesType: 'bar',
        tooltipFormatter: (data: any[]) => {
          return `${data[0].name}<br/>
          <span style='color: ${data[0].color}'>
            ${data[0].seriesName}：${data[0].value}
          </span><br/>`;
        },
      },
    },
  },
  ApiMap,
);

export default {
  performanceInterval: chartRender(chartMap.performanceInterval) as any,
  scriptError: chartRender(chartMap.scriptError) as any,
  pagePerformanceTrends: chartRender(chartMap.pagePerformanceTrends) as any,
  ajaxPerformanceTrends: chartRender(chartMap.ajaxPerformanceTrends) as any,
  loadPVUV: chartRender(chartMap.loadPVUV) as any,
};

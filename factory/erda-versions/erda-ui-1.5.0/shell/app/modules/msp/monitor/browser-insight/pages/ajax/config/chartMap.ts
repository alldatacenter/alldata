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
  moduleName: 'BIAjax',
  groupId: 'BIAjax',
};
const chartMap = merge(
  {
    sortTab: sortCreator(commonAttr.moduleName, 'sortTab'),
    sortList: {
      ...commonAttr,
      type: 'sortList',
      chartName: 'sortList',
    },
    rspTopN: {
      titleText: `${i18n.t('response time')} TOP5`,
      ...commonAttr,
      chartName: 'rspTopN',
      viewProps: {
        unitType: 'TIME',
      },
    },
    cpmTopN: {
      titleText: `${i18n.t('msp:throughput')} TOP5`,
      ...commonAttr,
      chartName: 'cpmTopN',
      viewProps: {
        unitType: 'CPM',
      },
    },
    postTopN: {
      titleText: i18n.t('msp:send data'),
      ...commonAttr,
      chartName: 'postTopN',
      viewProps: {
        unitType: 'CAPACITY',
      },
    },
    receiveTopN: {
      titleText: i18n.t('msp:receive data'),
      ...commonAttr,
      chartName: 'receiveTopN',
      viewProps: {
        unitType: 'CAPACITY',
      },
    },
    ajaxPerformanceTrends: {
      ...commonAttr,
      titleText: i18n.t('msp:ajax performance trends'),
      chartName: 'performanceTrends',
    },
    statusTopN: {
      titleText: i18n.t('msp:http status'),
      ...commonAttr,
      chartName: 'statusTopN',
    },
  },
  ApiMap,
);

export default {
  sortTab: sortRender(chartMap.sortTab) as any,
  sortList: sortRender(chartMap.sortList) as any,
  rspTopN: chartRender(chartMap.rspTopN) as any,
  cpmTopN: chartRender(chartMap.cpmTopN) as any,
  postTopN: chartRender(chartMap.postTopN) as any,
  receiveTopN: chartRender(chartMap.receiveTopN) as any,
  ajaxPerformanceTrends: chartRender(chartMap.ajaxPerformanceTrends) as any,
  statusTopN: chartRender(chartMap.statusTopN) as any,
};

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
import { chartRender } from 'mobile-insight/common/components/miRenderFactory';
import { ApiMap } from './apiConfig';
import i18n from 'i18n';

export const commonAttr = {
  moduleName: 'MIOverview',
  groupId: 'miOverview',
};
const chartMap = merge(
  {
    performanceInterval: {
      ...commonAttr,
      titleText: i18n.t('performance interval'),
      chartName: 'performanceInterval',
      viewProps: { unitType: 'TIME' },
    },
    pageError: {
      titleText: i18n.t('msp:page fault'),
      ...commonAttr,
      chartName: 'pageError',
    },
    pagePerformanceTrends: {
      ...commonAttr,
      titleText: i18n.t('msp:access performance trends'),
      groupId: '',
      chartName: 'performanceTrends',
    },
    reqPerformanceTrends: {
      ...commonAttr,
      chartName: 'ajaxPerformanceTrends',
      titleText: i18n.t('msp:request performance trends'),
    },
  },
  ApiMap,
);

export default {
  performanceInterval: chartRender(chartMap.performanceInterval) as any,
  pageError: chartRender(chartMap.pageError) as any,
  pagePerformanceTrends: chartRender(chartMap.pagePerformanceTrends) as any,
  reqPerformanceTrends: chartRender(chartMap.reqPerformanceTrends) as any,
};

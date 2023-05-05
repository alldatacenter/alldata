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
import { sortRender, chartRender } from 'browser-insight/common/components/biRenderFactory';
import { ApiMap } from './apiConfig';
import SlowTrace from '../exception-trace-panel';
import i18n from 'i18n';

const commonAttr = {
  moduleName: 'BIException',
  groupId: 'BIException',
};
const chartMap = merge(
  {
    sortTab: {
      moduleName: 'BIException',
      type: 'sortTab',
      tabList: [{ name: i18n.t('msp:error code'), key: 'code' }],
    },
    sortList: {
      moduleName: 'BIException',
      chartName: 'BIExceptionSort',
      type: 'sortList',
    },
    exception: {
      titleText: i18n.t('msp:access error'),
      ...commonAttr,
      chartName: 'exception',
    },
    slowTrack: {
      titleText: i18n.t('msp:error code tracking'),
      chartName: 'slow',
      ...commonAttr,
      viewRender: SlowTrace,
    },
  },
  ApiMap,
);

export default {
  sortTab: sortRender(chartMap.sortTab) as any,
  sortList: sortRender(chartMap.sortList) as any,
  exception: chartRender(chartMap.exception) as any,
  slowTrack: chartRender(chartMap.slowTrack) as any,
};

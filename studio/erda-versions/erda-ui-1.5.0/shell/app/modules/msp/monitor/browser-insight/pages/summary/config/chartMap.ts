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
import SummaryDetail from '../summary-detail';
import i18n from 'i18n';

const chartMap = merge(
  {
    sortTab: {
      moduleName: 'BISummary',
      type: 'sortTab',
      tabList: [
        { name: i18n.t('msp:access domain'), key: 'host' },
        { name: i18n.t('msp:access page'), key: 'url' },
        { name: i18n.t('msp:system'), key: 'os' },
        { name: i18n.t('msp:browser'), key: 'browser' },
        { name: i18n.t('msp:device'), key: 'device' },
      ],
    },
    sortList: {
      moduleName: 'BISummary',
      type: 'sortList',
      chartName: 'biSummarySort',
    },
    summaryDetail: {
      moduleName: 'BISummary',
      chartName: 'summaryDetail',
      viewRender: SummaryDetail,
    },
  },
  ApiMap,
);

export default {
  sortTab: sortRender(chartMap.sortTab) as any,
  sortList: sortRender(chartMap.sortList) as any,
  summaryDetail: chartRender(chartMap.summaryDetail) as any,
};
